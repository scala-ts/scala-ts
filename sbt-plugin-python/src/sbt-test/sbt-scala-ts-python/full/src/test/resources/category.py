# Generated by ScalaTS 0.5.20-SNAPSHOT: https://scala-ts.github.io/scala-ts/

import typing  # noqa: F401
import datetime  # noqa: F401
import time  # noqa: F401

from generated import ipsum  # noqa: F401
from generated.ipsum import Ipsum
from generated import lorem  # noqa: F401
from generated.lorem import Lorem


# Declare union Category
Category = typing.Union[Ipsum, Lorem]


class CategoryCompanion:
    @classmethod
    def Ipsum(self) -> Category:
        return ipsum.IpsumInhabitant

    @classmethod
    def Lorem(self) -> Category:
        return lorem.LoremInhabitant


CategoryKnownValues: typing.List[Category] = [
    CategoryCompanion.Ipsum(),
    CategoryCompanion.Lorem(),
]
