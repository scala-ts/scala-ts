# Generated by ScalaTS 0.5.17-SNAPSHOT: https://scala-ts.github.io/scala-ts/

from dataclasses import dataclass
import typing  # noqa: F401
import datetime  # noqa: F401
import time  # noqa: F401

from generated import classwithtypeargs  # noqa: F401
from generated.classwithtypeargs import ClassWithTypeArgs


# Declare interface ClassWithGenericField
@dataclass
class ClassWithGenericField:
    field: ClassWithTypeArgs[float]
