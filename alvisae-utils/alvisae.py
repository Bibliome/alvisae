#!/bin/env python

import argparse
import os
import os.path
import subprocess
import sys


class AlvisAEApp(argparse.ArgumentParser):
    def __init__(self, description, action):
        argparse.ArgumentParser.__init__(self, description=description)
        self.action = action
        self.add_argument('--java-home', metavar='PATH', dest='java_home', default=os.environ['JAVA_HOME'], help='Path to JDK home')
        self.add_argument('--alvisae-jar', metavar='JARFILE', dest='alvisae_jar', default='./AlvisAE-cli-modified.jar', help='Path to AlvisAE CLI JAR file')
        self.add_argument('--db-props', metavar='DBPROPS', dest='db_props', default='./db.props', help='Path to annotation database properties')

    def _begin_cli(self, args):
        return [
            os.path.join(args.java_home, 'bin', 'java'),
            '-jar',
            args.alvisae_jar,
            self.action,
            '-p',
            args.db_props
        ]

    def launch(self, args, end_cli):
        cli = self._begin_cli(args) + end_cli
        result = subprocess.run(cli, capture_output=True, encoding='utf8')
        self.handle_stderr(result.stderr)
        self.handle_stdout(result.stdout)

    def handle_stdout(self, stdout):
        sys.stdout.write(stdout)

    def handle_stderr(self, stderr):
        sys.stderr.write(stderr)


class PSQLApp(argparse.ArgumentParser):
    PG_PASS = os.path.expanduser('~/.pgpass')

    def __init__(self, description, sql_file):
        argparse.ArgumentParser.__init__(self, description=description)
        self.add_argument('--db-props', metavar='DBPROPS', dest='db_props', default='./db.props', help='Path to annotation database properties')
        self.sql_file = sql_file

    def run(self, args=None):
        args = self.parse_args(args)
        db_props = self._read_db_props(args)
        self._write_sql(args, db_props)
        existing_pgpass = self._ensure_pgpass(db_props)
        try:
            sys.stderr.write('running psql\n')
            result = subprocess.run(['psql', '-h', db_props['db.server'], '-p', db_props['db.port'], '-U', db_props['db.username'], '-w', '-f', self.sql_file, db_props['db.dbname']], capture_output=True, encoding='utf8')
            sys.stdout.write(result.stdout)
            sys.stderr.write(result.stderr)
        finally:
            if not existing_pgpass:
                sys.stderr.write('deleting %s\n' % PSQLApp.PG_PASS)
                os.remove(PSQLApp.PG_PASS)
        self._post_process(args)

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

    def _write_sql(self, args, db_props):
        with open(self.sql_file, 'w') as f:
            for sql in self._build_sql(args):
                f.write('SET search_path = %s;\n' % db_props['db.schema'])
                f.write(sql)
                f.write(';\n')

    def _ensure_pgpass(self, db_props):
        if os.path.exists(PSQLApp.PG_PASS):
            sys.stderr.write('using existing %s\n' % PSQLApp.PG_PASS)
            return True
        with open(PSQLApp.PG_PASS, 'w') as f:
            os.chmod(PSQLApp.PG_PASS, 0o600)
            sys.stderr.write('creating %s temporarily\n' % PSQLApp.PG_PASS)
            f.write('%s:%s:%s:%s:%s\n' % (db_props['db.server'], db_props['db.port'], db_props['db.dbname'], db_props['db.username'], db_props['db.password']))
        return False

    def _build_sql(self, args):
        raise NotImplementedError()

    def _post_process(self, ags):
        raise NotImplementedError()
