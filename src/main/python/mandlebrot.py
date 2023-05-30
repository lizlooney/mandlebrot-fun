"""Module mandlebrot calculates pixel values for mandlebrot fractals"""

# Copyright 2023 Liz Looney
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

from typing import Final

MAX_VALUE: Final[int] = 1000

class Mandlebrot():
    """Class that does calculations for Mandlebrot fractals."""

    size_in_pixels: int
    pixels_per_unit: float
    a_min: float
    b_min: float
    # HeyLiz values: int[]

    def __init__(self, size_in_pixels: int, a_center: float, b_center: float, size: float):
        self.size_in_pixels = size_in_pixels
        self.size = size
        self.pixels_per_unit = size_in_pixels // size
        self.a_min = a_center - size / 2
        self.b_min = b_center - size / 2
        self.values = [ 0 for i in range(size_in_pixels * size_in_pixels) ]
        self.calculate_pixel_values()

    def calculate_pixel_values(self):
        """Function that calculates the values for all pixels"""
        i = 0
        for y_coord in range(self.size_in_pixels):
            for x_coord in range(self.size_in_pixels):
                complex_number = complex(
                    self.a_min + x_coord / self.pixels_per_unit,
                    self.b_min + y_coord / self.pixels_per_unit)
                self.values[i] = Mandlebrot.calculate_value(complex_number)
                i += 1

    @staticmethod
    def calculate_value(complex_number: complex):
        """Function that calculates the value for the given complex number"""
        z_value = complex(complex_number)
        for i in range(MAX_VALUE):
            z_squared = z_value * z_value
            if abs(z_squared) >= 4:
                return i
            z_value = z_squared + complex_number
        return MAX_VALUE

    def show_values(self):
        """Function that shows the values as a grid of printable ascii characters"""
        i = 0
        for _ in range(self.size_in_pixels):
            for _ in range(self.size_in_pixels):
                value = 0x20 + self.values[i] % (0x7E - 0x20)
                print(chr(value), end='')
                i += 1
            print()


if __name__ == '__main__':
    mandlebrot = Mandlebrot(100, 0, 0, 4)
    mandlebrot.show_values()
