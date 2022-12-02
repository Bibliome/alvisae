#!/bin/env python

import alvisae


class Monitor(alvisae.PSQLApp):
    def __init__(self):
        alvisae.PSQLApp.__init__(self, 'Monitor campaigns', 'monitor.sql')
        self.add_argument('--campaigns', metavar='CID', dest='campaign_ids', required=True, help='Campaign identifiers (AlvisAE internal numeric, comma separated)')
        self.add_argument('--tasks', metavar='TASK', dest='tasks', required=False, default=None, help='Tasks to monitor (comma separated)')

    def _build_sql(self, args):
        select = 'campaign_id, c.name AS campaign_name, doc_id, doc.description AS doc_description, user_id, u.login AS annotator, t.name AS task, count(ans.id) AS versions'
        from_ = 'annotationset AS ans, campaign AS c, document AS doc, "user" AS u, taskdefinition AS t AND ans.task_id = t.id'
        where = 'ans.campaign_id IN (%s) AND campaign_id = c.id AND doc_id = doc.id AND user_id != 1 AND user_id = u.id' % args.campaign_ids
        group_by = 'campaign_id, c.name, doc_id, doc.description, user_id, u.login, t.name'
        if args.tasks is not None:
            where += ' AND t.name in (%s)' % (','.join(('\'' + t + '\'') for t in args.tasks))
        sql = 'SELECT %s FROM %s WHERE %s GROUP BY %s' % (select, from_, where, group_by)
        return [sql]

    def _post_process(self, args):
        pass


if __name__ == '__main__':
    Monitor().run()
