from typing import Dict

from core.io.csv import CsvWriter
from core.model.lines import Line


def write_line_capacities(line_capacities: Dict[Line, int], filename: str, header: str) -> None:
    CsvWriter.writeListStatic(filename,
                              list(line_capacities.keys()),
                              lambda l: [str(l.getId()), str(line_capacities[l])],
                              Line.getId,
                              header
                              )
