#!/bin/env python

import os
import json
import alvisae


SQL_FILE = 'update_schema.sql'


class RemoveAnnotation(alvisae.PSQLApp):
    def __init__(self):
        alvisae.PSQLApp.__init__(self, 'delete annotation', 'update_schema.sql')
        self.add_argument('--schema', metavar='FILE', dest='schema_file', required=True, help='New schema file')
        self.add_argument('--campaigns', metavar='CID', dest='campaign_ids', required=True, help='Campaign identifiers (AlvisAE internal numeric, comma separated)')

    def _build_sql(self, args):
        with open(args.schema_file) as f:
            schema = json.load(f)
        if 'schema' in schema:
            schema = schema['schema']
        str_schema = json.dumps(schema)
        esc_schema = str_schema.replace('\'', '\'\'')
        campaign_ids = ', '.join(str(int(cid)) for cid in args.campaign_ids.split(','))
        sql = 'UPDATE campaign SET schema = \'%s\' WHERE id in (%s);\n' % (esc_schema, campaign_ids)
        return [sql]

    def _post_process(self, args):
        pass


if __name__ == '__main__':
    RemoveAnnotation().run()


# UPDATE campaign SET schema = 'XXX' WHERE campaign_id = XXX;
