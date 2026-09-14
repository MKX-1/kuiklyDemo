"""直接从腾讯行情接口取一次真实报价，用于设计预览页（不经过自建后端）。"""
import urllib.request
import json

TOKENS = ["sh600519", "hk00700", "sz300750", "sz002594", "sh601318", "sz000858", "sh688981"]

opener = urllib.request.build_opener(urllib.request.ProxyHandler({}))
url = "http://qt.gtimg.cn/q=" + ",".join(TOKENS)
raw = opener.open(url, timeout=20).read().decode("gbk", errors="replace")

rows = []
for line in raw.split(";"):
    line = line.strip()
    if not line or "=" not in line:
        continue
    body = line.split("=", 1)[1].strip().strip('"')
    f = body.split("~")
    if len(f) < 46:
        continue
    name = f[1]
    code = f[2]
    price = float(f[3]) if f[3] else 0.0
    prev = float(f[4]) if f[4] else 0.0
    change = float(f[31]) if f[31] else 0.0
    change_pct = float(f[32]) if f[32] else 0.0
    high = float(f[33]) if len(f) > 33 and f[33] else 0.0
    low = float(f[34]) if len(f) > 34 and f[34] else 0.0
    turnover = float(f[38]) if len(f) > 38 and f[38] else 0.0
    pe = float(f[39]) if len(f) > 39 and f[39] else 0.0
    amplitude = float(f[43]) if len(f) > 43 and f[43] else 0.0
    float_cap = float(f[44]) if len(f) > 44 and f[44] else 0.0
    market_cap = float(f[45]) if len(f) > 45 and f[45] else 0.0
    rows.append(dict(token=line.split("_")[-1].split("=")[0][-2:] + code if False else code,
                     raw_token=line.split("=")[0].strip().replace("v_", ""),
                     name=name, code=code, price=price, prev=prev,
                     change=change, change_pct=change_pct, high=high, low=low,
                     turnover=turnover, pe=pe, amplitude=amplitude,
                     float_cap=float_cap, market_cap=market_cap))

print("取到 %d 条" % len(rows))
for r in rows:
    print("%-8s %-8s %10.2f  %+7.2f%%  换手%5.2f  振幅%5.2f  PE%7.2f" % (
        r["raw_token"], r["name"], r["price"], r["change_pct"], r["turnover"], r["amplitude"], r["pe"]))

open(r"C:\Users\rog\AppData\Local\Temp\quotes_live.json", "w", encoding="utf-8").write(
    json.dumps(rows, ensure_ascii=False, indent=1))
