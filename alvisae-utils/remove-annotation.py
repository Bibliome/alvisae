#!/bin/env python

import argparse
import os
import os.path
import subprocess
import sys


SQL_FILE = 'remove_annotations.sql'
PG_PASS = os.path.expanduser('~/.pgpass')


class RemoveAnnotation(argparse.ArgumentParser):
    def __init__(self):
        argparse.ArgumentParser.__init__(self, description='delete annotation')
        self.add_argument('--db-props', metavar='DBPROPS', dest='db_props', default='./db.props', help='Path to annotation database properties')
        self.add_argument('--campaign', metavar='CID', dest='campaign_id', required=True, help='Campaign identifier (AlvisAE internal numeric)')
        self.add_argument('--user', metavar='USER', dest='user', required=False, default=None, help='User name')
        self.add_argument('--task', metavar='TASK', dest='task', required=False, default=None, help='Task name')
        self.add_argument('--document', metavar='DOC', dest='doc', required=False, default=None, help='Task name')
        self.add_argument('--remove-assignment', action='store_true', dest='remove_assignment', default=False, help='also remove assignment')

    def run(self):
        args = self.parse_args()
        db_props = self._read_db_props(args)
        self._write_sql(args, db_props)
        existing_pgpass = self._ensure_pgpass(db_props)
        try:
            sys.stderr.write('running psql')
            result = subprocess.run(['psql', '-h', db_props['db.server'], '-p', db_props['db.port'], '-U', db_props['db.username'], '-w', '-f', SQL_FILE, db_props['db.dbname']], capture_output=True, encoding='utf8')
            sys.stdout.write(result.stdout)
            sys.stderr.write(result.stderr)
        finally:
            if not existing_pgpass:
                sys.stderr.write('deleting %s\n' % PG_PASS)
                os.remove(PG_PASS)

    def _write_sql(self, args, db_props):
        sql1, sql2 = self._build_sql(args)
        with open(SQL_FILE, 'w') as f:
            f.write('SET search_path = %s;\n' % db_props['db.schema'])
            f.write(sql1)
            f.write(';\n')
            if sql2 is not None:
                f.write(sql2)
                f.write(';\n')

    def _ensure_pgpass(self, db_props):
        if os.path.exists(PG_PASS):
            sys.stderr.write('using existing %s\n' % PG_PASS)
            return True
        with open(PG_PASS, 'w') as f:
            os.chmod(PG_PASS, 0o600)
            sys.stderr.write('creating %s temporarily\n' % PG_PASS)
            f.write('%s:%s:%s:%s:%s\n' % (db_props['db.server'], db_props['db.port'], db_props['db.dbname'], db_props['db.username'], db_props['db.password']))
        return False

    def _read_db_props(self, args):
        sys.stderr.write('reading %s\n' % args.db_props)
        props = {}
        with open(args.db_props) as f:
            for line in f:
                eq = line.index('=')
                if eq > 0:
                    key = line[:eq].strip()
                    value = line[(eq + 1):].strip()
                    props[key] = value
        return props

    def _build_sql(self, args):
        using = []
        using_task = []
        where = ['x.campaign_id = %s' % args.campaign_id]
        where_task = []
        if args.user is not None:
            using.append('"user" AS u')
            where.append('u.login IN (%s)' % (', '.join(('\'%s\'' % u.strip()) for u in args.user.split(','))))
            where.append('u.id = x.user_id')
        if args.task is not None:
            using_task.append('taskdefinition AS t')
            where_task.append('t.name IN (%s)' % (', '.join(('\'%s\'' % t.strip()) for t in args.task.split(','))))
            where_task.append('t.id = x.task_id')
        if args.doc is not None:
            where.append('x.doc_id IN (%s)' % (', '.join(('%s' % d.strip()) for d in args.doc.split(','))))
        sql1 = 'DELETE FROM annotationset AS x'
        if len(using) > 0 or len(using_task) > 0:
            sql1 += ' USING '
            sql1 += ', '.join(using + using_task)
        if len(where) > 0 or len(where_task) > 0:
            sql1 += ' WHERE '
            sql1 += ' AND '.join(('(%s)' % w) for w in (where + where_task))
        if args.remove_assignment:
            sql2 = 'DELETE FROM documentassignment as x'
            if len(using) > 0:
                sql2 += ' USING '
                sql2 += ', '.join(using)
            if len(where) > 0:
                sql2 += ' WHERE '
                sql2 += ' AND '.join(('(%s)' % w) for w in where)
        else:
            sql2 = None
        return sql1, sql2


if __name__ == '__main__':
    RemoveAnnotation().run()

# DELETE FROM annotationset      AS aset USING "user" AS u WHERE u.login = 'XXX' AND u.id = aset.user_id AND aset.doc_id = XXX AND aset.campaign_id = XXX;
# DELETE FROM documentassignment AS dass USING "user" AS u WHERE u.login = 'XXX' AND u.id = dass.user_id AND dass.doc_id = XXX AND dass.campaign_id = XXX;
