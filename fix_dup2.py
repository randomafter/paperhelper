# -*- coding: utf-8 -*-
path = 'd:/work/paperhelper/frontend/src/views/Workspace.vue'
with open(path, 'r', encoding='utf-8') as f:
    lines = f.readlines()

target = '              const raw = inlineResult.value.text\n'
for i in range(len(lines)-1):
    if lines[i] == target and i+3 < len(lines) and lines[i+3] == target:
        print(f'Removing duplicate at lines {i+4}-{i+6}')
        del lines[i+3:i+6]
        break

with open(path, 'w', encoding='utf-8') as f:
    f.writelines(lines)
print('Done')
