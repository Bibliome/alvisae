import alvisnlp
import sys
import collections


class Occurrences:
    def __init__(self):
        self.occurrences = set()
        self.match_boundaries = collections.defaultdict(lambda: collections.defaultdict(Occurrences))


def get_element(t, role, occurrences):
    elt = t.args[role]
    form = elt.features['form']
    if CF:
        form = form.lower()
    type_ = elt.features['type']
    occ = occurrences[(form, type_)]
    occ.occurrences.add(elt)
    return occ


def get_match_level(match_boundaries, match_type):
    if match_boundaries == 'boundaries-none':
        return match_boundaries
    if match_boundaries == 'boundaries-exact':
        if match_type == 'type-identical':
            return None
        return match_type
    if match_type == 'type-identical':
        return match_boundaries
    return '%s, %s' % (match_boundaries, match_type)


CORPUS = alvisnlp.Corpus.parse_json(sys.stdin)
cf = CORPUS.params['caseFolding']
CF = cf == 'yes' or cf == 'y' or cf == 'true'
ENTITIES = collections.defaultdict(Occurrences)
for doc in CORPUS.documents:
    for sec in doc.sections:
        for t in sec.relations['check-annotations_entity-occurrences'].tuples:
            entity_occ = get_element(t, 'entity', ENTITIES)
            match_boundaries = t.features['match-boundaries']
            match_type = t.features['match-type']
            match_level = get_match_level(match_boundaries, match_type)
            if match_level is None:
                continue
            matches = entity_occ.match_boundaries[match_level]
            if match_boundaries == 'boundaries-none':
                a = t.args['occurrence']
                matches[(a.form, '')].occurrences.add(a)
            else:
                get_element(t, 'match', matches)


def tuple_str(t):
    if isinstance(t, alvisnlp.Tuple):
        doc = t.relation.section.document
        start = t.features['start_']
        end = t.features['end_']
    else:
        doc = t.section.document
        start = str(t.start)
        end = str(t.end)
    return '[c=%s (%s), d=%s (%s), o=%s-%s]' % (doc.features['campaign-id'], doc.features['campaign-name'], doc.identifier, doc.features['external-id'], start, end)


def occ_str(occ):
    return '; '.join(tuple_str(t) for t in occ.occurrences)


sys.stdout.write('\t'.join([
    'ENTITY_FORM',
    'ENTITY_TYPE',
    'N_ENTITY_OCCURRENCES',
    'ENTITY_OCCURRENCES',
    'MATCH_LEVEL',
    'MATCH_FORM',
    'MATCH_TYPE',
    'N_MATCH_OCCURRENCES',
    'MATCH_OCCURRENCES'
]))
sys.stdout.write('\n')
for (entity_form, entity_type), entity_occ in ENTITIES.items():
    entity_occ_n = str(len(entity_occ.occurrences))
    entity_occ_str = occ_str(entity_occ)
    for match_level, matches in entity_occ.match_boundaries.items():
        for (match_form, match_type), match_occ in matches.items():
            sys.stdout.write('\t'.join([
                entity_form,
                entity_type,
                entity_occ_n,
                entity_occ_str,
                match_level,
                match_form,
                match_type,
                str(len(match_occ.occurrences)),
                occ_str(match_occ)
            ]))
            sys.stdout.write('\n')
            match_boundaries = ''
        entity_form = ''
        entity_type = ''
        entity_occ_n = ''
        entity_occ_str = ''
