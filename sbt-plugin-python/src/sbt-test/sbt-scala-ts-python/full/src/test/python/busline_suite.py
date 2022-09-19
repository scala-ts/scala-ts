import unittest

from generated.busline import BusLine


class BusLineSuite(unittest.TestCase):

    def test(self):
        line1 = BusLine(
            id=1,
            name='Line #1',
            stopIds=[])

        self.assertEqual(line1.id, 1)
        self.assertEqual(line1.name, 'Line #1')
        self.assertEqual(line1.stopIds, [])

        line2 = BusLine(
            id=2,
            name='Line #2',
            stopIds=['A', 'B', 'C'])

        self.assertEqual(line2.id, 2)
        self.assertEqual(line2.name, 'Line #2')
        self.assertEqual(line2.stopIds, ['A', 'B', 'C'])

        self.assertNotEqual(line1, line2)
        
