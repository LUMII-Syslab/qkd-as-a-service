import os
import random
import string
import time

dir_path = "/tmp/qkd-mock"
if not os.path.exists(dir_path):
    os.makedirs(dir_path)


def clean_dir(path):
    for entry in os.listdir(path):
        os.remove(os.path.join(path, entry))


clean_dir(dir_path)


def random_filename(length) -> str:
    return ''.join(random.choice(string.ascii_letters) for _ in range(length))


def create_random_file():
    existing_file_count = len(os.listdir(dir_path))
    prefix = str(existing_file_count + 1)
    file_path = os.path.join(dir_path, prefix + "-" + random_filename(8))
    with open(file_path, "wb") as f:
        f.write(os.urandom(256))
        print("Created file: " + file_path)


for i in range(50):
    create_random_file()
while True:
    create_random_file()
    time.sleep(0.5)
