import unittest

from generated import alabama
from generated.alabama import Alabama

from generated import alaska
from generated.alaska import Alaska

from generated import state
from generated.state import StateCompanion


class StateSuite(unittest.TestCase):

    def test(self):
        catAlabama: Alabama = StateCompanion.Alabama()
        self.assertEqual(catAlabama, alabama.AlabamaInhabitant)

        catAlaska: Alaska = StateCompanion.Alaska()
        self.assertEqual(catAlaska, alaska.AlaskaInhabitant)

        self.assertEqual(state.StateKnownValues, ['AL', 'AK'])
