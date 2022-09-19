import unittest

from generated.busline import BusLine
from generated.trainline import TrainLine
from generated.transport import Transport


class TransportSuite(unittest.TestCase):

    def test(self):
        busline: Transport = BusLine(
            id=1,
            name='Line #1',
            stopIds=['A', 'B', 'C'])

        trainline: Transport = TrainLine(
            name='Train',
            startStationId='start',
            endStationId='end')
