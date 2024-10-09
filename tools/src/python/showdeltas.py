# This is a sample Python script.
import argparse
import sys
import os
from dataclasses import dataclass

input_poly_file = None
input_spec_file = None
input_util_file = None


def calculate_delta(prev, curr):
    result = dict()
    has_deltas = False
    for k in curr.keys():
        if k.startswith("#") or k.startswith('$'):
            result[k] = curr[k]
        elif prev[k] != curr[k]:
            result[k] = curr[k]
            has_deltas = True

    if has_deltas:
        return result
    else:
        return None


def parse_spec(spec, step):
    species = {
        '$step': step,
        '$description': spec[:25],
        '#polyname': spec[:21],
        '#year': spec[21:25],
        '#layer_type': spec[26:27],
        '#genus_index': int(spec[28:30]),
        '$genus': spec[31:33].strip(),
        '$sp0': spec[34:37].strip(),
        'psp0t': float(spec[37:42]),
        '$sp1': spec[42:45].strip(),
        'psp1t': float(spec[45:50]),
        '$sp2': spec[50:53].strip(),
        'psp2t': float(spec[53:58]),
        '$sp3': spec[58:61].strip(),
        'psp3t': float(spec[61:66]),
        'site_index': float(spec[66:72]),
        'dh': float(spec[72:78]),
        'total_age': float(spec[78:84]),
        'yabh': float(spec[84:90]),
        'ytbh': float(spec[90:96]),
        '$is_primary_species': int(spec[96:98]),
        'site_curve_number': int(spec[98:])
    }

    return species


def opt_float(e, k):
    return e[k] if k in e.keys() else ""


def opt_int(e, k):
    return e[k] if k in e.keys() else ""


def format_spec(spec):
    return "{:-2} {:4} {} {} {:-2} {:2} {:9.6} {:9.6} {:9.6} {:9.6} {:9.6} {} {:3}".format(
        spec['$step'], spec['#year'], spec['#polyname'], spec['#layer_type'], spec['#genus_index'], spec['$genus'],
        opt_float(spec, 'site_index'), opt_float(spec, 'total_age'), opt_float(spec, 'dh'),
        opt_float(spec, 'yabh'), opt_float(spec, 'ytbh'),
        ('P' if spec['$is_primary_species'] else ' '),
        opt_int(spec, 'site_curve_number'))


def format_spec_header():
    return "st year description           T Gx G   site idx total age  dom hgt  yrs at bh yrs to bh P StC"


def parse_util(util, step):
    utilization = {
        '$step': step,
        '$description': util[:25],
        '#polyname': util[:21],
        '#year': util[21:25],
        '#layer_type': util[26:27],
        '#genus_index': int(util[27:30]),
        '$genus': util[31:33].strip(),
        '#utilization_class_index': int(util[33:36]),
        'basal_area': float(util[36:45]),
        'live_trees_per_hectare': float(util[45:54]),
        'lorey_height': float(util[54:63]),
        'whole_stem_volume': float(util[63:72]),
        'close_util_volume': float(util[72:81]),
        'cu_volume_less_decay': float(util[81:90]),
        'cu_volume_less_decay_wastage': float(util[90:99]),
        'cu_volume_less_decay_wastage_breakage': float(util[99:108]),
        'quadratic_mean_diameter_bh': float(util[108:])
    }

    return utilization


def format_util(util):
    return "{:-2} {:4} {} {} {:-2} {:2} {:2} {:9.6} {:9.6} {:9.6} {:9.6} {:9.6} {:9.6} {:9.6} {:9.6} {:9.6}".format(
        util['$step'], util['#year'], util['#polyname'], util['#layer_type'], util['#genus_index'], util['$genus'],
        util['#utilization_class_index'],
        opt_float(util, 'basal_area'), opt_float(util, 'live_trees_per_hectare'), opt_float(util, 'lorey_height'),
        opt_float(util, 'whole_stem_volume'), opt_float(util, 'close_util_volume'), opt_float(util, 'cu_volume_less_decay'),
        opt_float(util, 'cu_volume_less_decay_wastage'), opt_float(util, 'cu_volume_less_decay_wastage_breakage'),
        opt_float(util, 'quadratic_mean_diameter_bh'))


def format_util_header():
    return "st year description           T Gx G  UC base area  live tph  lorey ht    ws_vol" \
           "    cu_vol   cud_vol  cuwd_vol cuwdb_vol    qmd_bh"


@dataclass(frozen=True)
class Key:
    polyname: str
    year: int
    layer_type: str
    genus_index: int
    utilization_class_index: int


def build_poly_year_key(e):
    uci = e['#utilization_class_index'] if '#utilization_class_index' in e.keys() else None
    return Key(polyname=e['#polyname'],
               year=e['#year'],
               layer_type=e['#layer_type'],
               genus_index=e['#genus_index'],
               utilization_class_index=uci)


def build_poly_key(poly_year_key):
    return Key(polyname=poly_year_key.polyname,
               year=0,
               layer_type=poly_year_key.layer_type,
               genus_index=poly_year_key.genus_index,
               utilization_class_index=poly_year_key.utilization_class_index)


def get_sort_order(e):
    if e['#layer_type'] == 'P':
        value = 1
    else:
        value = 2
    value = value * 100 + e['#genus_index']
    if '#utilization_class_index' in e:
        value = value * 100 + e['#utilization_class_index']
    return value


def collect_deltas(location, filename_pattern, first_step_number):
    global input_poly_file, input_spec_file, input_util_file

    specs = []
    utils = []

    with (
        open(os.path.join(location, filename_pattern.replace('%', 'p'))) as input_poly_file,
        open(os.path.join(location, filename_pattern.replace('%', 's'))) as input_spec_file,
        open(os.path.join(location, filename_pattern.replace('%', 'u'))) as input_util_file
    ):
        poly_line = input_poly_file.readline().rstrip()

        # per polygon
        while len(poly_line) > 0:

            poly = poly_line[:21]
            poly_year = poly_line[:25]

            prev_poly_specs = None
            prev_poly_utils = None

            # per polygon year
            while True:

                # per polygon year grow step
                step = first_step_number
                while True:
                    (curr_species_texts, curr_util_texts) = get_next_poly_set(poly_year)

                    curr_poly_specs = []
                    for n_spec in range(0, len(curr_species_texts)):
                        curr_spec = parse_spec(curr_species_texts[n_spec], step)
                        delta_spec = curr_spec
                        if prev_poly_specs is not None:
                            prev_poly_spec = prev_poly_specs[n_spec]
                            delta_spec = calculate_delta(prev_poly_spec, curr_spec)
                            if delta_spec is not None:
                                specs.append(curr_spec)
                        else:
                            specs.append(curr_spec)
                        curr_poly_specs.append(curr_spec)

                    curr_poly_utils = []
                    for n_util in range(0, len(curr_util_texts)):
                        curr_util = parse_util(curr_util_texts[n_util], step)
                        delta_util = curr_util
                        if prev_poly_utils is not None:
                            prev_poly_util = prev_poly_utils[n_util]
                            delta_util = calculate_delta(prev_poly_util, curr_util)
                            if delta_util is not None:
                                utils.append(curr_util)
                        else:
                            utils.append(curr_util)
                        curr_poly_utils.append(curr_util)

                    prev_poly_specs = curr_poly_specs
                    prev_poly_utils = curr_poly_utils

                    poly_line = input_poly_file.readline().rstrip()

                    if not poly_line.startswith(poly_year):
                        poly_year = poly_line[:25]
                        break

                    step += 1

                if not poly_line.startswith(poly):
                    poly = poly_line[:21]
                    break

    return specs, utils


def get_next_poly_set(poly_year):
    global input_spec_file, input_util_file

    spec_state = get_next_set(poly_year, input_spec_file)
    util_state = get_next_set(poly_year, input_util_file)

    return spec_state, util_state


def get_next_set(context, file):
    next_set = []
    next_line = file.readline().rstrip()
    while next_line.startswith(context) and len(next_line) > len(context):
        next_set.append(next_line)
        next_line = file.readline().rstrip()

    return next_set


def merge(source, target):
    for k in source.keys():
        if k not in target.keys():
            target[k] = source[k]


def merge_vdyp8_step_results(results, sorter):
    collapsed_records = []

    n = 0
    while n < len(results):
        poly_desc = results[n]['$description']

        poly_records = [results[n]]
        n += 1
        while n < len(results) and results[n]['$description'] == poly_desc:
            poly_records.append(results[n])
            n += 1

        records_by_key = dict()
        for r in poly_records:
            key = build_poly_year_key(r)
            if key not in records_by_key:
                records_by_key[key] = []
            records_by_key[key].insert(0, r)

        collapsed_records_of_poly = []
        for k in records_by_key:
            last_record = records_by_key[k][0]
            pn = 1
            while pn < len(records_by_key[k]):
                merge(records_by_key[k][pn], last_record)
                pn += 1
            collapsed_records_of_poly.append(last_record)

        collapsed_records_of_poly = sorted(collapsed_records_of_poly, key=sorter)
        collapsed_records.extend(collapsed_records_of_poly)

    return collapsed_records


def compare_records(e1, e2):
    assert e1['#polyname'] == e2['#polyname']
    return get_sort_order(e1) - get_sort_order(e2)


def copy_to_year(e, year):
    e_copy = e.copy()
    e_copy['#year'] = year
    return e_copy


def compare(vdyp7_data_list, vdyp8_data_list, tolerance):

    results = []

    i7 = 0
    i8 = 0
    prev7_records = dict()
    prev8_records = dict()
    while i7 < len(vdyp7_data_list) or i8 < len(vdyp8_data_list):

        vdyp7_poly_year_key = build_poly_year_key(vdyp7_data_list[i7]) if i7 < len(vdyp7_data_list) else None
        vdyp7_poly_key = build_poly_key(vdyp7_poly_year_key)
        vdyp8_poly_year_key = build_poly_year_key(vdyp8_data_list[i8]) if i8 < len(vdyp8_data_list) else None
        vdyp8_poly_key = build_poly_key(vdyp8_poly_year_key)

        if vdyp7_poly_year_key is None or compare_records(vdyp7_data_list[i7], vdyp8_data_list[i8]) > 0:
            prev_record = copy_to_year(prev7_records.get(vdyp8_poly_key), vdyp8_data_list[i8]['#year'])
            vdyp7_data_list.insert(i7, prev_record)
        elif vdyp8_poly_year_key is None or compare_records(vdyp8_data_list[i8], vdyp7_data_list[i7]) > 0:
            prev_record = copy_to_year(prev8_records.get(vdyp7_poly_key), vdyp7_data_list[i7]['#year'])
            vdyp8_data_list.insert(i8, prev_record)

        prev7_records[vdyp7_poly_key] = vdyp7_data_list[i7]
        i7 += 1
        prev8_records[vdyp8_poly_key] = vdyp8_data_list[i8]
        i8 += 1

    assert len(vdyp7_data_list) == len(vdyp8_data_list)

    for i in range(0, len(vdyp7_data_list)):
        vdyp7_data = vdyp7_data_list[i]
        vdyp8_data = vdyp8_data_list[i]

        vdyp7_poly_year_key = build_poly_year_key(vdyp7_data)
        vdyp8_poly_year_key = build_poly_year_key(vdyp8_data)
        assert vdyp7_poly_year_key == vdyp8_poly_year_key

        result = dict()
        has_differences = False
        for k in vdyp7_data.keys():
            if not k.startswith("$") and not k.startswith("#"):
                if isinstance(vdyp7_data[k], float):
                    if vdyp7_data[k] != vdyp8_data[k]:
                        if vdyp7_data[k] == 0:
                            result[k] = vdyp8_data[k]
                        else:
                            result[k] = vdyp8_data[k] / vdyp7_data[k]
                            if abs(result[k]) - 1.0 <= tolerance:
                                result[k] = 1.0
                            else:
                                has_differences = True
                    else:
                        result[k] = 1.0
                elif isinstance(vdyp7_data[k], int):
                    if vdyp7_data[k] != vdyp8_data[k]:
                        result[k] = vdyp8_data[k] - vdyp7_data[k]
                        has_differences = True
                    else:
                        result[k] = 0
            else:
                result[k] = vdyp8_data[k]

        if has_differences:
            results.append(result)

    return results


def write_specs(specs, file):
    rn = 0
    for spec in specs:
        if rn % 50 == 0:
            file.write(f"{format_spec_header()}\n")
        file.write(f"{format_spec(spec)}\n")
        rn += 1


def write_utils(utils, file):
    rn = 0
    for util in utils:
        if rn % 30 == 0:
            file.write(f"{format_util_header()}\n")
        file.write(f"{format_util(util)}\n")
        rn += 1


if __name__ == '__main__':
    parser = argparse.ArgumentParser(
        prog='VDYP Result Delta Calculator',
        description='This program compares the results of two VDYP runs and produces files showing the differences')

    parser.add_argument('vdyp7_folder')
    parser.add_argument('vdyp8_folder')
    parser.add_argument('-p', '--pattern', default='v%_grow2.dat')
    parser.add_argument('-t', '--tolerance', default=0, type=float)

    args = parser.parse_args()

    vdyp7_folder = args.vdyp7_folder
    vdyp8_folder = args.vdyp8_folder

    with (
        open(os.path.join(vdyp7_folder, args.pattern.replace('%', 's') + '.full'), 'w') as output7_full_spec_file,
        open(os.path.join(vdyp7_folder, args.pattern.replace('%', 'u') + '.full'), 'w') as output7_full_util_file,
        open(os.path.join(vdyp8_folder, args.pattern.replace('%', 's') + '.diff'), 'w') as output_spec_delta_file,
        open(os.path.join(vdyp8_folder, args.pattern.replace('%', 's') + '.full'), 'w') as output8_full_spec_file,
        open(os.path.join(vdyp8_folder, args.pattern.replace('%', 's') + '.year'), 'w') as output_merged_spec_file,
        open(os.path.join(vdyp8_folder, args.pattern.replace('%', 'u') + '.diff'), 'w') as output_util_delta_file,
        open(os.path.join(vdyp8_folder, args.pattern.replace('%', 'u') + '.full'), 'w') as output8_full_util_file,
        open(os.path.join(vdyp8_folder, args.pattern.replace('%', 'u') + '.year'), 'w') as output_merged_util_file
    ):
        (vdyp7_year_specs, vdyp7_year_utils) = collect_deltas(vdyp7_folder, args.pattern, 14)
        (vdyp8_step_specs, vdyp8_step_utils) = collect_deltas(vdyp8_folder, args.pattern, 1)

        write_specs(vdyp7_year_specs, output7_full_spec_file)
        write_specs(vdyp8_step_specs, output8_full_spec_file)
        write_utils(vdyp7_year_utils, output7_full_util_file)
        write_utils(vdyp8_step_utils, output8_full_util_file)

        vdyp8_year_specs = merge_vdyp8_step_results(vdyp8_step_specs, sorter=lambda s: get_sort_order(s))
        vdyp8_year_utils = merge_vdyp8_step_results(vdyp8_step_utils, sorter=lambda u: get_sort_order(u))

        write_specs(vdyp8_year_specs, output_merged_spec_file)
        write_utils(vdyp8_year_utils, output_merged_util_file)

        spec_diffs = compare(vdyp7_year_specs, vdyp8_year_specs, args.tolerance)
        util_diffs = compare(vdyp7_year_utils, vdyp8_year_utils, args.tolerance)

        write_specs(spec_diffs, output_spec_delta_file)
        write_utils(util_diffs, output_util_delta_file)
    pass
