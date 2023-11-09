# psutil version coded by Jurgen Pfeifer
# Extends compatability, should run on Debian and Ubuntu
#!/usr/bin/env python3
# -*- coding: utf-8 -*-
#
import time
import psutil as PS
import socket
import board
import digitalio
import adafruit_ssd1306

#
from PIL import Image, ImageDraw, ImageFont

#
WIDTH = 128
HEIGHT = 64
FONTSIZE = 20
#
LOOPTIME = 1.0
# I2C 통신
i2c = board.I2C()
oled = adafruit_ssd1306.SSD1306_I2C(WIDTH, HEIGHT, i2c, addr=0x3C)
# Clear display.
oled.fill(0)
oled.show()
# Create blank image for drawing.
# Make sure to create image with mode '1' for 1-bit color.
image = Image.new("1", (oled.width, oled.height))
# Get drawing object to draw on image.
draw = ImageDraw.Draw(image)
# Draw a black filled box
draw.rectangle((0, 0, oled.width, oled.height), outline=0, fill=0)
padding = 0
top = padding
bottom = oled.height - 40
x = 33
# font = ImageFont.load_default()
font1 = ImageFont.truetype("godoMaum.ttf", FONTSIZE)
font2 = ImageFont.truetype("godoMaum.ttf", FONTSIZE - 6)


def OLED_show(a):
    if a == 1:
        draw.text((x, top), "warning!!!", font=font1, fill=255)
        draw.text((x, bottom), "차량 운전자 \n졸음운전 중!!!", font=font2, fill=255)
        # Display image
        oled.image(image)
        oled.show()
        time.sleep(LOOPTIME)
    else:
        oled.fill(0)
        oled.show()


if __name__ == "__main__":
    a = 1
    OLED_show(a)
