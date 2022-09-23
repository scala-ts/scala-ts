import unittest

from datetime import datetime

from generated import lorem
from generated.lorem import Lorem

from generated import ipsum
from generated.ipsum import Ipsum

from generated import category
from generated.category import CategoryCompanion


class CategorySuite(unittest.TestCase):

    def test(self):
        catLorem: Lorem = CategoryCompanion.Lorem()
        self.assertEqual(catLorem, lorem.LoremInhabitant)

        catIpsum: Ipsum = CategoryCompanion.Ipsum()
        self.assertEqual(catIpsum, ipsum.IpsumInhabitant)

        self.assertEqual(category.CategoryKnownValues, ['Ipsum', 'Lorem'])
