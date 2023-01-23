import os

search_dir = "/tmp/qkd-mock"
if not os.path.exists(search_dir):
    os.makedirs(search_dir)

print("Files in directory: " + search_dir)
print(os.listdir(search_dir))

files = [os.path.join(search_dir, f) for f in os.listdir(search_dir)]
files = list(filter(os.path.isfile, files))
print(list(files))
files.sort(key=lambda x: os.path.getmtime(x))
files.reverse()

for file in files:
    print(os.path.basename(file))
