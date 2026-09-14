import os, sys, time
from collections import defaultdict

def walk_size(root, day_bucket=None, top_bucket=None):
    """返回 (总字节, 按创建日汇总dict, 按顶层子目录汇总dict)"""
    total = 0
    by_day = defaultdict(int)
    by_top = defaultdict(int)
    cnt = 0
    root = root.rstrip('\\')
    stack = [root]
    while stack:
        d = stack.pop()
        try:
            it = os.scandir(d)
        except (PermissionError, FileNotFoundError, OSError):
            continue
        with it:
            for e in it:
                try:
                    if e.is_dir(follow_symlinks=False):
                        stack.append(e.path)
                    elif e.is_file(follow_symlinks=False):
                        st = e.stat(follow_symlinks=False)
                        sz = st.st_size
                        total += sz
                        cnt += 1
                        if by_day is not None:
                            by_day[time.strftime('%Y-%m-%d', time.localtime(st.st_ctime))] += sz
                        if by_top is not None:
                            rel = e.path[len(root) + 1:]
                            top = rel.split('\\', 1)[0] if '\\' in rel else '(root files)'
                            by_top[top] += sz
                except (PermissionError, FileNotFoundError, OSError):
                    continue
    return total, by_day, by_top, cnt

def gb(b):
    return b / 1024 / 1024 / 1024

out = []
def p(s=''):
    out.append(str(s))

# 磁盘
import shutil
u = shutil.disk_usage('C:\\')
p('C: free=%.2f GB  total=%.2f GB  used=%.2f GB' % (gb(u.free), gb(u.total), gb(u.used)))
p()

targets = [
    (r'C:\Windows\SoftwareDistribution\Download', True, True),
    (r'C:\$WINDOWS.~BT', False, False),
    (r'C:\$WinREAgent', False, False),
    (r'C:\Users\rog\.android', True, True),
    (r'C:\Users\rog\.gradle', True, True),
    (r'C:\Users\rog\AppData\Local\Temp', True, False),
    (r'C:\Users\rog\AppData\Local\Android', True, True),
    (r'C:\Users\rog\AppData\Local\Google', True, False),
    (r'C:\Users\rog\Documents\Codex\Android', True, True),
    (r'C:\Users\rog\AppData\Local\Packages', False, False),
    (r'C:\Windows\Temp', False, False),
    (r'C:\Windows\Installer', False, False),
    (r'C:\$Recycle.Bin', False, False),
]

for root, want_day, want_top in targets:
    if not os.path.exists(root):
        p('MISSING  ' + root)
        continue
    t0 = time.time()
    total, by_day, by_top, cnt = walk_size(root, want_day, want_top)
    p('%-55s %9.3f GB  (%d files, %.1fs)' % (root, gb(total), cnt, time.time() - t0))
    if want_day and by_day:
        recent = sorted(by_day.items(), reverse=True)[:6]
        p('    创建日分布(top6): ' + ', '.join('%s=%.2fGB' % (d, gb(v)) for d, v in recent))
    if want_top and by_top:
        top = sorted(by_top.items(), key=lambda kv: -kv[1])[:8]
        p('    子目录(top8): ' + ', '.join('%s=%.2fGB' % (k, gb(v)) for k, v in top))
    p()

open(r'C:\Users\rog\AppData\Local\Temp\d5.txt', 'w', encoding='utf-8').write('\n'.join(out))
print('done')
