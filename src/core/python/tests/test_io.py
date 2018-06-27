import difflib
import errno
import filecmp
import os
import shutil
import sys
import unittest

from core.io.aperiodic_ean import AperiodicEANReader, AperiodicEANWriter
from core.io.config import ConfigReader
from core.io.lines import LineReader, LineWriter
from core.io.od import ODReader, ODWriter
from core.io.periodic_ean import PeriodicEANReader, PeriodicEANWriter
from core.io.ptn import PTNReader, PTNWriter
from core.io.statistic import StatisticReader, StatisticWriter
from core.io.trip import TripReader, TripWriter
from core.io.vehicleSchedule import VehicleScheduleReader, VehicleScheduleWriter
from core.model.activityType import ActivityType
from core.model.eventType import EventType
from core.model.lines import LineDirection
from core.model.vehicle_scheduling import TripType
from core.util.config import Config
from core.util.statistic import Statistic
from .context import core


class IOTest(unittest.TestCase):

    input_path = os.path.abspath(os.path.join(os.path.join(os.path.dirname(__file__),"resources"), "dataset"))
    output_path = os.path.abspath(os.path.join(os.path.dirname(__file__), "output"))
    base_path = os.path.join(input_path, "basis")

    @staticmethod
    def compare_files(file_name_1: str, file_name_2: str) -> bool:
        with open(file_name_1, "r") as f1, open(file_name_2, "r") as f2:
            file_content_1 = f1.readlines()
            file_content_2 = f2.readlines()
            if len(file_content_1) != len(file_content_2):
                return False
            for line1, line2 in zip(file_content_1, file_content_2):
                if line1 != line2:
                    print("Files {} and {} differ in line {}/{}".format(file_name_1, file_name_2, line1, line2))
                    return False
        return True

    def setUp(self):
        try:
            os.makedirs(self.output_path)
        except OSError as exc:
            if exc.errno == errno.EEXIST and os.path.isdir(self.output_path):
                pass
            else:
                raise

    def tearDown(self):
        shutil.rmtree(self.output_path)

    def test_config(self):
        config_path = os.path.abspath(os.path.join(os.path.join(
            os.path.join(os.path.join(os.path.dirname(__file__),"resources"), "dataset"), "basis"), "Config.cnf"))
        config = Config()
        ConfigReader.read(config_path, config=config)
        self.assertEqual(353, len(config.data))

    def test_ptn(self):
        stop_path = os.path.join(self.base_path, "Stop.giv")
        link_path = os.path.join(self.base_path, "Edge.giv")
        load_path = os.path.join(self.base_path, "Load.giv")
        headway_path = os.path.join(self.base_path, "Headway.giv")
        ptn = PTNReader.read(read_loads=True, read_headways=True, stop_file_name=stop_path, link_file_name=link_path,
                             load_file_name=load_path, headway_file_name=headway_path, directed=False,
                             conversion_factor_coordinates=1, conversion_factor_length=1)
        self.assertEqual(8, len(ptn.getNodes()))
        self.assertEqual(8, len(ptn.getEdges()))
        self.assertEqual(3, ptn.getEdge(1).getLowerFrequencyBound())
        self.assertEqual(5, ptn.getEdge(1).getHeadway())
        output_stop_path = os.path.join(self.output_path, "Stop.giv")
        output_link_path = os.path.join(self.output_path, "Edge.giv")
        output_load_path = os.path.join(self.output_path, "Load.giv")
        output_headway_path = os.path.join(self.output_path, "Headway.giv")
        PTNWriter.write(ptn, write_loads=True, write_headways=True, stop_file_name=output_stop_path,
                        stop_header="stop-id; short-name; long-name; x-coordinate; y-coordinate",
                        link_file_name=output_link_path,
                        link_header="edge-id; left-stop-id; right-stop-id; length; lower-bound; upper-bound",
                        load_file_name=output_load_path, load_header="link_index; load; min_freq; max_freq",
                        headway_file_name=output_headway_path, headway_header="edge-id; headway")
        self.assertTrue(IOTest.compare_files(stop_path, output_stop_path))
        self.assertTrue(IOTest.compare_files(link_path, output_link_path))
        self.assertTrue(IOTest.compare_files(load_path, output_load_path))
        self.assertTrue(IOTest.compare_files(headway_path, output_headway_path))

    def test_od(self):
        od_path = os.path.join(self.base_path, "OD.giv")
        od = ODReader.read(None, 8, od_path)
        self.assertEqual(2622, od.computeNumberOfPassengers())
        self.assertEqual(46, len(od.getODPairs()))
        self.assertEqual(10, od.getValue(1, 2))
        stop_path = os.path.join(self.base_path, "Stop.giv")
        ptn = PTNReader.read(stop_file_name=stop_path, read_links=False, directed=False,
                             conversion_factor_coordinates=1)
        output_od_path = os.path.join(self.output_path, "OD.giv")
        ODWriter.write(ptn, od, file_name=output_od_path, header="left-stop-id; right-stop-id; customers")
        self.assertTrue(IOTest.compare_files(od_path, output_od_path))

    def test_line_pool(self):
        stop_path = os.path.join(self.base_path, "Stop.giv")
        link_path = os.path.join(self.base_path, "Edge.giv")
        ptn = PTNReader.read(stop_file_name=stop_path, link_file_name=link_path, directed=False,
                             conversion_factor_coordinates=1, conversion_factor_length=1)
        pool_path = os.path.join(self.base_path, "Pool.giv")
        cost_path = os.path.join(self.base_path, "Pool-Cost.giv")
        pool = LineReader.read(ptn, read_frequencies=False, line_file_name=pool_path, line_cost_file_name=cost_path)
        self.assertEqual(8, len(pool.getLines()))
        self.assertEqual(ptn.getEdge(6), pool.getLine(1).getLinePath().getEdges()[1])
        self.assertEqual(1.8, pool.getLine(4).getLength())
        self.assertEqual(0, pool.getLine(1).getFrequency())
        self.assertEqual(3.8, pool.getLine(7).getCost())
        output_pool_path = os.path.join(self.output_path, "Pool.giv")
        output_cost_path = os.path.join(self.output_path, "Pool-Cost.giv")
        LineWriter.write(pool, write_line_concept=False, pool_file_name=output_pool_path,
                         pool_header="line-id; edge-order; edge", cost_file_name=output_cost_path,
                         cost_header="line-id; length; cost")
        self.assertTrue(IOTest.compare_files(pool_path, output_pool_path))
        self.assertTrue(IOTest.compare_files(cost_path, output_cost_path))

    def test_line_concept(self):
        stop_path = os.path.join(self.base_path, "Stop.giv")
        link_path = os.path.join(self.base_path, "Edge.giv")
        ptn = PTNReader.read(stop_file_name=stop_path, link_file_name=link_path, directed=False,
                             conversion_factor_coordinates=1, conversion_factor_length=1)
        lc_path = os.path.join(os.path.join(self.input_path, "line-planning"), "Line-Concept.lin")
        lc = LineReader.read(ptn, read_costs=False, line_file_name=lc_path)
        self.assertEqual(4, lc.getLine(2).getFrequency())
        self.assertEqual(4, len(lc.getLineConcept()))
        output_lc_path = os.path.join(self.output_path, "Line-Concept.lin")
        LineWriter.write(lc, write_pool=False, write_costs=False, concept_file_name=output_lc_path,
                         concept_header="line-id;edge-order;edge-id;frequency")
        self.assertTrue(IOTest.compare_files(lc_path, output_lc_path))

    def test_periodic_ean(self):
        event_path = os.path.join(os.path.join(self.input_path, "timetabling"), "Events-periodic.giv")
        activity_path = os.path.join(os.path.join(self.input_path, "timetabling"), "Activities-periodic.giv")
        timetable_path = os.path.join(os.path.join(self.input_path, "timetabling"), "Timetable-periodic.tim")
        ean, timetable = PeriodicEANReader.read(read_timetable=True, event_file_name=event_path,
                                                activity_file_name=activity_path, timetable_file_name=timetable_path,
                                                time_units_per_minute=1, period_length=60)
        self.assertEqual(168, len(ean.getNodes()))
        self.assertEqual(EventType.ARRIVAL, ean.getNode(2).getType())
        self.assertEqual(LineDirection.FORWARDS, ean.getNode(4).getDirection())
        self.assertEqual(7, ean.getNode(5).getTime())
        self.assertEqual(168, len(timetable))
        self.assertEqual(50, timetable.get(ean.getNode(21)))
        self.assertEqual(60, timetable.getPeriod())
        self.assertEqual(858, len(ean.getEdges()))
        self.assertEqual(ActivityType.CHANGE, ean.getEdge(850).getType())
        output_event_path = os.path.join(self.output_path, "Events-periodic.giv")
        output_activity_path = os.path.join(self.output_path, "Activities-periodic.giv")
        output_timetable_path = os.path.join(self.output_path, "Timetable-periodic.tim")
        PeriodicEANWriter.write(ean, write_timetable=True, events_file_name=output_event_path,
                                activities_file_name=output_activity_path, timetable_file_name=output_timetable_path,
                                events_header="event_id; type; stop-id; line-id; passengers; line-direction; "
                                              "line-freq-repetition",
                                activities_header="activity_index; type; from_event; to_event; lower_bound; "
                                                  "upper_bound; passengers",
                                timetable_header="event-id; time")
        self.assertTrue(IOTest.compare_files(event_path, output_event_path))
        self.assertTrue(IOTest.compare_files(activity_path, output_activity_path))
        self.assertTrue(IOTest.compare_files(timetable_path, output_timetable_path))

    def test_aperiodic_ean(self):
        event_path = os.path.join(os.path.join(self.input_path, "delay-management"), "Events-expanded.giv")
        activity_path = os.path.join(os.path.join(self.input_path, "delay-management"), "Activities-expanded.giv")
        timetable_path = os.path.join(os.path.join(self.input_path, "delay-management"), "Timetable-disposition.tim")
        ean, timetable = AperiodicEANReader.read(event_file_name=event_path, activity_file_name=activity_path,
                                                 time_units_per_minute=1)
        self.assertEqual(652, len(ean.getNodes()))
        self.assertEqual(EventType.DEPARTURE, ean.getNode(2).getType())
        self.assertEqual(42600, ean.getNode(84).getTime())
        self.assertEqual(652, len(timetable))
        self.assertEqual(31860, timetable.get(ean.getNode(85)))
        self.assertEqual(578, len(ean.getEdges()))
        self.assertEqual(ActivityType.CHANGE, ean.getEdge(576).getType())
        output_event_path = os.path.join(self.output_path, "Events-expanded.giv")
        output_activity_path = os.path.join(self.output_path, "Activities-expanded.giv")
        AperiodicEANWriter.write(ean, write_activities=True, events_file_name=output_event_path,
                                 activities_file_name=output_activity_path,
                                 events_header="event-id; periodic-id; type; time; passengers; stop-id",
                                 activities_header="activity-id; periodic-id; type; tail-event-id; head-event-id; "
                                                   "lower-bound; upper-bound; passengers")
        self.assertTrue(IOTest.compare_files(event_path, output_event_path))
        self.assertTrue(IOTest.compare_files(activity_path, output_activity_path))
        ean, timetable = AperiodicEANReader.read(event_file_name=event_path, activity_file_name=activity_path,
                                                 time_units_per_minute=1, read_disposition_timetable=True,
                                                 timetable_file_name=timetable_path)
        self.assertEqual(652, len(ean.getNodes()))
        self.assertEqual(EventType.DEPARTURE, ean.getNode(2).getType())
        self.assertEqual(43345, ean.getNode(84).getTime())
        self.assertEqual(652, len(timetable))
        self.assertEqual(32460, timetable.get(ean.getNode(85)))
        self.assertEqual(578, len(ean.getEdges()))
        self.assertEqual(ActivityType.CHANGE, ean.getEdge(576).getType())
        output_timetable_path = os.path.join(self.output_path, "Timetable-disposition.tim")
        AperiodicEANWriter.write(ean, write_events=False, write_disposition_timetable=True, timetable=timetable,
                                 timetable_file_name=output_timetable_path, timetable_header="event-id; time")
        self.assertTrue(IOTest.compare_files(timetable_path, output_timetable_path))

    def test_statistic(self):
        statistic_path = os.path.join(os.path.join(self.input_path, "statistic"), "statistic.sta")
        statistic = Statistic()
        StatisticReader.read(statistic=statistic, file_name=statistic_path)
        self.assertEqual(10, len(statistic.getData()))
        self.assertEqual(6.87, statistic.getDoubleValue("tim_time_average"))
        output_statistic_path = os.path.join(self.output_path, "statistic.sta")
        StatisticWriter.write(statistic=statistic, file_name=output_statistic_path)
        self.assertTrue(IOTest.compare_files(statistic_path, output_statistic_path))

    def test_trips(self):
        trip_path = os.path.join(os.path.join(self.input_path, "delay-management"), "Trips.giv")
        trips = TripReader.read(file_name=trip_path)
        self.assertEqual(110, len(trips))
        self.assertEqual(36060, trips[2].getStartTime())
        output_trip_path = os.path.join(self.output_path, "Trips.giv")
        TripWriter.write(trips, file_name=output_trip_path, header="start-ID; periodic-start-ID; start-station; "
                                                                   "start-time; end-ID; periodic-end-ID; end-station; "
                                                                   "end-time; line")
        self.assertTrue(IOTest.compare_files(trip_path, output_trip_path))

    def test_vehicle_schedule(self):
        vs_path = os.path.join(os.path.join(self.input_path, "vehicle-scheduling"), "Vehicle_Schedules.vs")
        vs = VehicleScheduleReader.read(file_name=vs_path)
        self.assertEqual(6, len(vs.getCirculations()))
        self.assertEqual(1, len(vs.getCirculation(2).getVehicleTourList()))
        self.assertEqual(32, len(vs.getCirculation(1).getVehicleTourList()[0].getTripList()))
        self.assertEqual(TripType.TRIP, vs.getCirculation(2).getVehicleTour(2).getTrip(51).getTripType())
        output_vs_path = os.path.join(self.output_path, "Vehicle_Schedules.vs")
        VehicleScheduleWriter.write(vs, file_name=output_vs_path,
                                    header="circulation-ID; vehicle-ID; trip-number of this vehicle; type; start-ID; "
                                           "periodic-start-ID; start-station; start-time; end-ID; periodic-end-id; "
                                           "end-station; end-time; line")
        self.assertTrue(IOTest.compare_files(vs_path, output_vs_path))



