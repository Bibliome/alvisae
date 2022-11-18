#!/bin/env python

import alvisae
import json
import sys


class CreateCampaign(alvisae.AlvisAEApp):
    def __init__(self):
        alvisae.AlvisAEApp.__init__(self, 'creates a campaign', '--create-campaign')
        self.add_argument('--name', metavar='NAME', dest='campaign_name', required=True, help='Campaign name')
        self.add_argument('--schema', metavar='SCHEMA', dest='schema', default='./schema.json', help='Path to annotation schema JSON file')
        self.add_argument('--workflow', metavar='WORKFLOW', dest='workflow', default='./workflow.xml', help='Path to annotation workflow XML file')
        self.add_argument('--store-id', metavar='FILE', dest='store_id', required=False, help='Path to file where to write the created campaign id')

    def run(self):
        self.args = self.parse_args()
        with open(self.args.schema) as f:
            schema = json.load(f)
        if 'schema' not in schema:
            schema = {'schema': schema}
        schema_file = '.schema.json'
        with open(schema_file, 'w') as f:
            json.dump(schema, f, indent=4)
        end_cli = [
            '-c',
            self.args.campaign_name,
            '-s',
            schema_file,
            '-w',
            self.args.workflow
        ]
        self.launch(self.args, end_cli)

    def handle_stdout(self, stdout):
        if self.args.store_id is None:
            sys.stdout.write(stdout)
        else:
            cid, *_ = stdout.split('\t')
            with open(self.args.store_id, 'w') as f:
                f.write(cid)
            sys.stderr.write('campaign identifier written in ' + self.args.store_id)


if __name__ == '__main__':
    CreateCampaign().run()
