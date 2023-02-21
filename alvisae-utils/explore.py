#!/bin/env python

import os
import alvisae


class Explore(alvisae.PSQLApp):
    def __init__(self):
        alvisae.PSQLApp.__init__(self, 'explore database', 'explore.sql')
        self.add_argument('--sql', metavar='SQL', dest='sql', action='append', default=[], required=False, help='SQL command')
        self.add_argument('--list-tables', dest='list_tables', action='store_true', default=False, required=False, help='List tables')
        self.add_argument('--detail-table', metavar='TABLE', dest='detail_table', required=False, help='Detail specified table')

    def _build_sql(self, args):
        if args.list_tables:
            return ('\\dt',)
        if args.detail_table:
            return ('\\d %s' % args.detail_table,)
        return args.sql

    def _post_process(self, args):
        pass


if __name__ == '__main__':
    Explore().run()
