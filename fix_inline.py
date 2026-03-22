# -*- coding: utf-8 -*-
path = 'd:/work/paperhelper/frontend/src/views/Workspace.vue'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

old = "const isInline = ['polish', 'expand', 'rewrite', 'check'].includes(tabKey)"
new_ = "const isInline = ['polish', 'expand', 'rewrite', 'check', 'continue', 'scene', 'dialogue', 'outline', 'title'].includes(tabKey)"

if old in content:
    content = content.replace(old, new_, 1)
    print('Fix applied')
else:
    print('NOT FOUND')

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)
print('Done')
