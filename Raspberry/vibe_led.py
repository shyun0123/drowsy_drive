# LED, 진동모터
import RPi.GPIO as GPIO
import time

motor_pin = 12
LED_pin = 25

GPIO.setwarnings(False)
GPIO.setmode(GPIO.BCM)
GPIO.setup(motor_pin, GPIO.OUT)
GPIO.setup(LED_pin, GPIO.OUT)

for i in range(1, 11, 1):
    GPIO.output(motor_pin, GPIO.HIGH)
    GPIO.output(LED_pin, 1)
    time.sleep(0.3)
    GPIO.output(motor_pin, GPIO.LOW)
    GPIO.output(LED_pin, 0)
    time.sleep(0.3)

GPIO.cleanup
