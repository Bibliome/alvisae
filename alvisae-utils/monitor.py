#!/bin/env python

import alvisae
import collections


class Monitor(alvisae.PSQLApp):
    def __init__(self):
        alvisae.PSQLApp.__init__(self, 'Monitor campaigns', 'monitor.sql')
        self.add_argument('--campaigns', metavar='CID', dest='campaign_ids', required=True, help='Campaign identifiers (AlvisAE internal numeric, comma separated)')
        self.add_argument('--tasks', metavar='TASK', dest='tasks', required=False, default=None, help='Tasks to monitor (comma separated)')
        self.add_argument('--aggregate-documents', dest='aggregate_documents', required=False, default=False, action='store_true', help='Aggregate documents')

    def _build_sql(self, args):
        select = 'ans.campaign_id, c.name AS campaign_name, doc_id, doc.description AS doc_description, user_id, u.login AS annotator, t.name AS task, count(ans.id) AS versions'
        from_ = 'annotationset AS ans, campaign AS c, document AS doc, "user" AS u, taskdefinition AS t'
        where = 'ans.campaign_id IN (%s) AND ans.campaign_id = c.id AND doc_id = doc.id AND user_id != 1 AND user_id = u.id AND ans.task_id = t.id' % args.campaign_ids
        group_by = 'ans.campaign_id, c.name, doc_id, doc.description, user_id, u.login, t.name'
        if args.tasks is not None:
            where += ' AND t.name in (%s)' % (','.join(('\'' + t + '\'') for t in args.tasks))
        sql = 'SELECT %s FROM %s WHERE %s GROUP BY %s' % (select, from_, where, group_by)
        copy = '\\copy (%s) TO \'%s\' WITH CSV DELIMITER \'\t\' HEADER' % (sql, 'monitor.csv')
        return [copy]

    def _read_data(self):
        with open('monitor.csv') as f:
            data = []
            header = None
            for line in f:
                cols = list(c.strip() for c in line.split('\t'))
                if header is None:
                    header = cols
                else:
                    row = dict(zip(header, cols))
                    data.append(row)
        return header, data

    def _post_process(self, args):
        if args.aggregate_documents:
            header, data = self._read_data()
            agg_header = list(h for h in header if h not in ('doc_id', 'doc_description', 'versions'))
            agg = collections.defaultdict(list)
            for row in data:
                k = tuple(row[h] for h in agg_header)
                agg[k].append(int(row['versions']))
            with open('monitor.csv', 'w') as f:
                f.write('\t'.join(agg_header))
                f.write('\tdoc_versions\n')
                for k, v in agg.items():
                    f.write('\t'.join(k))
                    v.sort()
                    f.write('\t%s\n' % ','.join(str(i) for i in v))
            

if __name__ == '__main__':
    Monitor().run()
