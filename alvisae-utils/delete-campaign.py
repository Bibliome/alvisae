#!/bin/env python


import alvisae


class DeleteCampaign(alvisae.PSQLApp):
    DEPENDENT_TABLES = (
        'campaignannotator',
        'annotationset',
        'campaigndocument',
        'taskprecedency',
        'taskdefinition'
    )
    
    def __init__(self):
        alvisae.PSQLApp.__init__(self, 'delete campaign', 'delete_campaign.sql')
        self.add_argument('--campaigns', metavar='CID', dest='campaign_ids', required=True, help='Campaign identifiers (AlvisAE internal numeric, comma separated)')

    def _build_sql(self, args):
        result = list(('DELETE FROM %s WHERE campaign_id IN (%s)' % (t, args.campaign_ids)) for t in DeleteCampaign.DEPENDENT_TABLES)
        result.append('DELETE FROM campaign WHERE id IN (%s)' % args.campaign_ids)
        return result

    def _post_process(self, args):
        pass


if __name__ == '__main__':
    DeleteCampaign().run()
