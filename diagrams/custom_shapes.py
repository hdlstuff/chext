
from xml.etree import ElementTree
from xml.dom import minidom

from typing import *

import abc
import dataclasses


class NodeCreator(abc.ABC):
    @abc.abstractmethod
    def create_node(self, parent: ElementTree.Element) -> None:
        ...


def _dataclass_to_element(parent: ElementTree.Element, tag: str, dc: Any) -> ElementTree.Element:
    assert (dataclasses.is_dataclass(dc))

    attrib = {}

    for field in dataclasses.fields(dc):
        name: str = field.name

        if (isinstance(field.metadata, dict)):
            metadata: Dict = field.metadata

            if (metadata.get("skip", False)):
                continue

            name = metadata.get("name", field.name)

        attrib[name] = str(getattr(dc, field.name))

    return ElementTree.SubElement(parent, tag, attrib)


def _skipped():
    return dataclasses.field(default=None, metadata={"skip": True})


def _name(name: str):
    return dataclasses.field(default=None, metadata={"name": name})


@dataclasses.dataclass
class _Path_move(NodeCreator):
    x: float
    y: float

    def create_node(self, parent: ElementTree.Element) -> None:
        _dataclass_to_element(parent, "move", self)


@dataclasses.dataclass
class _Path_line(NodeCreator):
    x: float
    y: float

    def create_node(self, parent: ElementTree.Element) -> None:
        _dataclass_to_element(parent, "line", self)


@dataclasses.dataclass
class _Path_quad(NodeCreator):
    x1: float
    y1: float
    x2: float
    y2: float

    def create_node(self, parent: ElementTree.Element) -> None:
        _dataclass_to_element(parent, "quad", self)


@dataclasses.dataclass
class _Path_curve(NodeCreator):
    x1: float
    y1: float
    x2: float
    y2: float
    x3: float
    y3: float

    def create_node(self, parent: ElementTree.Element) -> None:
        _dataclass_to_element(parent, "quad", self)


@dataclasses.dataclass
class _Path_close(NodeCreator):
    def create_node(self, parent: ElementTree.Element) -> None:
        _dataclass_to_element(parent, "close", self)


@dataclasses.dataclass
class Point:
    x: float
    y: float


@dataclasses.dataclass
class Size:
    w: float
    h: float


class Path(NodeCreator):
    def __init__(self) -> None:
        self._commands: List[NodeCreator] = []

    def move(self, pt: Point) -> None:
        self._commands.append(_Path_move(pt.x, pt.y))

    def lineTo(self, pt: Point) -> None:
        self._commands.append(_Path_line(pt.x, pt.y))

    def line(self, pt1: Point, pt2: Point) -> None:
        self.move(pt1)
        self.lineTo(pt2)

    def quadTo(self, pt: Point, control: Point) -> None:
        self._commands.append(_Path_quad(control.x, control.y, pt.x, pt.y))

    def quad(self, pt1: Point, pt2: Point, control: Point) -> None:
        self.move(pt1)
        self.quadTo(pt2, control)

    def curveTo(self, pt: Point, control1: Point, control2: Point) -> None:
        self._commands.append(_Path_quad(
            control1.x, control1.y, control2.x, control2.y, pt.x, pt.y))

    def curve(self, pt1: Point, pt2: Point, control1: Point, control2: Point) -> None:
        self.move(pt1)
        self.curveTo(pt2, control1, control2)

    def close(self) -> None:
        self._commands.append(_Path_close())

    @property
    def commands(self):
        return self._commands

    def create_node(self, parent: ElementTree.Element) -> None:
        path = ElementTree.SubElement(parent, "path")
        for command in self.commands:
            command.create_node(path)


class _RectHelpers:
    @classmethod
    def corners(cls, pt1: Point, pt2: Point, arcsize: float = 0.0):
        return cls(pt1.x, pt1.y, pt2.x - pt1.x, pt2.y - pt1.y, arcsize)

    @classmethod
    def left(cls, pt: Point, sz: Size, arcsize: float = 0.0):
        return cls(pt.x, pt.y, sz.w, sz.h, arcsize)

    @classmethod
    def center(cls, pt: Point, sz: Size, arcsize: float = 0.0):
        return cls(pt.x - sz.w / 2, pt.y - sz.h / 2, sz.w, sz.h, arcsize)

    @classmethod
    def left_radius(cls, pt: Point, radius: float, arcsize: float = 0.0):
        return cls(pt.x, pt.y, radius * 2, radius * 2, arcsize)

    @classmethod
    def center_radius(cls, pt: Point, radius: float, arcsize: float = 0.0):
        return cls(pt.x - radius, pt.y - radius, radius * 2, radius * 2, arcsize)


@dataclasses.dataclass
class Rect(NodeCreator, _RectHelpers):
    x: float
    y: float
    w: float
    h: float
    arcsize: float = _skipped()

    def create_node(self, parent: ElementTree.Element) -> None:
        _dataclass_to_element(parent, "rect", self)


@dataclasses.dataclass
class RoundRect(NodeCreator, _RectHelpers):
    x: float
    y: float
    w: float
    h: float
    arcsize: float = 0.0

    def create_node(self, parent: ElementTree.Element) -> None:
        _dataclass_to_element(parent, "roundrect", self)


@dataclasses.dataclass
class Ellipse(NodeCreator, _RectHelpers):
    x: float
    y: float
    w: float
    h: float
    arcsize: float = _skipped()

    def create_node(self, parent: ElementTree.Element) -> None:
        _dataclass_to_element(parent, "ellipse", self)


@dataclasses.dataclass
class Text(NodeCreator):
    text: str = _name("str")
    x: float = 0
    y: float = 0
    align: Union[Literal["left"], Literal["right"],
                 Literal["center"]] = "center"
    valign: Union[Literal["top"], Literal["middle"],
                  Literal["bottom"]] = "middle"
    vertical: int = 0

    def create_node(self, parent: ElementTree.Element) -> None:
        _dataclass_to_element(parent, "text", self)


class Shape:
    def __init__(
        self,
        name: str,
        size: Size,
        aspect: Union[Literal["fixed"], Literal["variable"]] = "fixed"
    ) -> None:
        self._name = name
        self._size = size
        self._aspect = aspect

        self._foreground: List[NodeCreator] = []
        self._background: List[NodeCreator] = []
        self._connections: List[Point] = []

    @property
    def name(self):
        return self._name

    @property
    def size(self):
        return self._size

    @property
    def aspect(self):
        return self._aspect

    @property
    def foreground(self):
        return self._foreground

    @property
    def background(self):
        return self._background

    @property
    def connections(self):
        return self._connections

    def create_xml(self) -> bytes:
        root = ElementTree.Element("shape", {
            "name": self.name,
            "w": str(self.size.w),
            "h": str(self.size.h),
            "aspect": self.aspect,
            "strokewidth": "inherit"
        })

        connections = ElementTree.SubElement(root, "connections")
        for pt in self.connections:
            ElementTree.SubElement(connections, "constraint", {
                "x": str(pt.x / self.size.w),
                "y": str(pt.y / self.size.h),
                "perimeter": "0"
            })

        background = ElementTree.SubElement(root, "background")
        for x in self.background:
            x.create_node(background)
        ElementTree.SubElement(background, "fillstroke")

        foreground = ElementTree.SubElement(root, "foreground")
        for x in self.foreground:
            x.create_node(foreground)
        ElementTree.SubElement(foreground, "fillstroke")

        ElementTree.indent(root, space="    ")
        return ElementTree.tostring(root)


def muxLike(baseName: str, n: int, reflect: bool = False) -> Shape:
    shape = Shape(
        f"{baseName}_{n}",
        Size(20, n * 20),
        "fixed"
    )

    path = Path()

    if not reflect:
        path.move(Point(0, 0))
        path.lineTo(Point(0, n * 20))
        path.lineTo(Point(20, n * 20 - 10))
        path.lineTo(Point(20, 10))
        path.close()

        shape.foreground.append(path)

        # shape.foreground.append(Text(f"{n}-to-1", 5, n * 20 / 2))

        for i in range(n):
            shape.connections.append(Point(0, 10 + (i * 20)))
        shape.connections.append(Point(20, n * 20 / 2))
        shape.connections.append(Point(10, 5))
        shape.connections.append(Point(10, n * 20 - 5))

    else:
        path.move(Point(0, 10))
        path.lineTo(Point(0, n * 20 - 10))
        path.lineTo(Point(20, n * 20))
        path.lineTo(Point(20, 0))
        path.close()

        shape.foreground.append(path)
        
        # shape.foreground.append(Text(f"1-to-{n}", 5, n * 20 / 2))

        for i in range(n):
            shape.connections.append(Point(20, 10 + (i * 20)))
        shape.connections.append(Point(0, n * 20 / 2))
        shape.connections.append(Point(10, 5))
        shape.connections.append(Point(10, n * 20 - 5))

    return shape


if __name__ == "__main__":
    for i in range(16):
        mux = muxLike("mux", i + 1)
        with open(f"mux_{i + 1}.xml", "wb") as f:
            f.write(mux.create_xml())

        demux = muxLike("demux", i + 1, True)
        with open(f"demux_{i + 1}.xml", "wb") as f:
            f.write(demux.create_xml())
