# Generated by ScalaTS 0.5.14-SNAPSHOT: https://scala-ts.github.io/scala-ts/

from dataclasses import dataclass
import typing  # noqa: F401
import datetime  # noqa: F401

import feature  # noqa: F401
from feature import Feature


# Declare interface NamedFeature
@dataclass
class NamedFeature:
    name: str
    feature: Feature
