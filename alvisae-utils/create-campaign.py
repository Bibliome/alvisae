#!/bin/env python

import alvisae
import json


class CreateCampaign(alvisae.AlvisAEApp):
    def __init__(self):
        alvisae.AlvisAEApp.__init__(self, 'creates a campaign', '--create-campaign')
        self.add_argument('--name', metavar='NAME', dest='campaign_name', required=True, help='Campaign name')
        self.add_argument('--schema', metavar='SCHEMA', dest='schema', default='./schema.json', help='Path to annotation schema JSON file')
        self.add_argument('--workflow', metavar='WORKFLOW', dest='workflow', default='./workflow.xml', help='Path to annotation workflow XML file')

    def run(self):
        args = self.parse_args()
        with open(args.schema) as f:
            schema = json.load(f)
        if 'schema' not in schema:
            schema = {'schema': schema}
        schema_file = '.schema.json'
        with open(schema_file, 'w') as f:
            json.dump(schema, f, indent=4)
        end_cli = [
            '-c',
            args.campaign_name,
            '-s',
            schema_file,
            '-w',
            args.workflow
        ]
        self.launch(args, end_cli)


if __name__ == '__main__':
    CreateCampaign().run()
