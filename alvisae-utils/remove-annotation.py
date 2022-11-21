#!/bin/env python

import os
import alvisae


class RemoveAnnotation(alvisae.PSQLApp):
    def __init__(self):
        alvisae.PSQLApp.__init__(self, 'delete annotation', 'remove_annotations.sql')
        self.add_argument('--campaign', metavar='CID', dest='campaign_id', required=True, help='Campaign identifier (AlvisAE internal numeric)')
        self.add_argument('--user', metavar='USER', dest='user', required=False, default=None, help='User name')
        self.add_argument('--task', metavar='TASK', dest='task', required=False, default=None, help='Task name')
        self.add_argument('--document', metavar='DOC', dest='doc', required=False, default=None, help='Document identifier (AlvisAE internal numeric)')
        self.add_argument('--remove-assignment', action='store_true', dest='remove_assignment', default=False, help='also remove assignment')

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
