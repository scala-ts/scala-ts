import unittest

from generated import barnum
from generated.barnum import BarNum

from generated import foolure
from generated.foolure import FooLure

from generated import feature
from generated.feature import FeatureCompanion


class FeatureSuite(unittest.TestCase):

    def test(self):
        catBarNum: BarNum = FeatureCompanion.BarNum()
        self.assertEqual(catBarNum, barnum.BarNumInhabitant)

        catFooLure: FooLure = FeatureCompanion.FooLure()
        self.assertEqual(catFooLure, foolure.FooLureInhabitant)

        self.assertEqual(feature.FeatureKnownValues, ['BarNum', 'FooLure'])
