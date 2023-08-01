import alvisnlp
import sys
import collections

CORPUS = alvisnlp.Corpus.parse_json(sys.stdin)
PROPERTY = CORPUS.params['property']
DICT = collections.defaultdict(lambda: collections.defaultdict(list))
for doc in CORPUS.documents:
    for sec in doc.sections:
        for rel in sec.relations:
            for t in rel.tuples:
                form = t.features.get_last('form')
                value = ','.join(sorted(t.features.get(PROPERTY)))
                DICT[form][value].append(t)


def tuple_str(t):
    doc = t.relation.section.document
    return '[%s/%s %s-%s]' % (doc.identifier, doc.features.get_last('external-id'), t.features.get_last('start_'), t.features.get_last('end_'))


with open(CORPUS.params['outFile'], 'w') as f:
    for form, values in DICT.items():
        if len(values) >= 2:
            for value, tuples in values.items():
                f.write('\t'.join([form, values, '; '.join(tuple_str(t) for t in tuples)]))
