from drawio import *
from typing import *


def muxLike(baseName: str, n: int, reflect: bool = False) -> Shape:
    shape = Shape(
        f"{baseName}_{n}",
        Size(20, n * 20),
        "variable"
    )

    path = Path()

    if not reflect:
        path.move(Point(0, 0))
        path.lineTo(Point(0, n * 20))
        path.lineTo(Point(20, n * 20 - 10))
        path.lineTo(Point(20, 10))
        path.close()

        shape.foreground.append(path)
        shape.foreground.append(FillStroke())

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
        shape.foreground.append(FillStroke())

        # shape.foreground.append(Text(f"1-to-{n}", 5, n * 20 / 2))

        for i in range(n):
            shape.connections.append(Point(20, 10 + (i * 20)))
        shape.connections.append(Point(0, n * 20 / 2))
        shape.connections.append(Point(10, 5))
        shape.connections.append(Point(10, n * 20 - 5))

    return shape


if __name__ == "__main__":
    shapeLibrary = ShapeLibrary()

    for i in range(2, 9):
        mux = muxLike("mux", i)
        shapeLibrary.shapes.append(mux)

    for i in range(2, 9):
        demux = muxLike("demux", i, True)
        shapeLibrary.shapes.append(demux)

    with open("elastic_shapes.xml", "w") as f:
        f.write(shapeLibrary.to_str())
