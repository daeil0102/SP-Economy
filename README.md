<h1>SP-Economyn</h1>

<p>해당 프로젝트는 프록시용 이코노미 시스템 입니다</p>
<p>해당 플러그인을 사용하기 위해선 SP-Framework가 필요합니다</p>
<p>해당 프로젝트는은 money, shop, usershop 으로 나눠져 있습니다</p>
<p>해당 프로젝트는 미완성된 프로젝트 입니다 일부 오류가 있을 수 있습니다</p>

<h2>플러그인 버전</h2>

- 플러그인 : spigot 1.12+

<h2>라이센스</h2>

Copyright (c) 2026 Teujaem

1. 상업적 이용이 가능합니다.
2. 2차 수정이 불가능 합니다. (fork 포함)
3. 2차 배포가 불가능 합니다.

<p>해당 프로젝트는 스크립트 제작한 시스템을 AI를 활용하여 플러그인으로 재구성 했습니다</p>
<P>일부 코드를 제외하고 대부분의 코드는 AI가 작성 했습니다</P>
<p>원본 스크립트는 해당 디스코드에서 확인 하실 수 있습니다</p>
https://discord.gg/pZN24fnZs2

<h2>config</h2>

<p>bukkit/plugins/SP-Shop/config.yml</p>

```
# ==============================
#   Shop Plugin Configuration
# ==============================

# 전역 설정
open-enabled: true            # false 로 두면 일반 유저는 "/상점 열기" 를 쓸 수 없습니다 (관리자는 항상 가능).

# 채팅 프리픽스 / GUI 제목 (& 색상 코드 사용 가능)
messages:
  prefix-shop: "&a[ Shop ] &f"
  prefix-help: "&a[ Shop Help ] &f"
  prefix-error: "&c[ Error ] &f"

gui-titles:
  main: "&a[ 상점 ] "
  item-edit: "&a[ 상점 아이템설정 ] "
  price-edit-buy: "&a[ 상점 구매 가격설정 ] "
  price-edit-sell: "&a[ 상점 판매 가격설정 ] "
  trade-edit: "&a[ 상점 거래설정 ] "
  stock-edit: "&a[ 상점 재고설정 ] "
  price-setting: "&a[ 가격 ] "
  stock-setting: "&a[ 재고 ] "
  trade-setting: "&a[ 거래 ] "

# 자동 시세변동 / 재고초기화 스케줄러 주기(틱). 1200틱 = 60초.
scheduler-period-ticks: 1200
```

<p>bukkit/plugins/SP-UserShop/config.yml</p>

```
# 유저상점 설정

# 판매 등록 시 수수료 (%)
fee-percent: 5

# 기본 판매 만료 기간 (등록 시점부터)
expire:
  days: 2
  hours: 0
  minutes: 0

# ProxyEvent로 구매 소식을 다른 서버에 알릴 때 사용할 이 서버의 이름 (로그/알림용)
server-name: "server-1"

messages:
  prefix-shop: "&6&l[&a&lUserShop&6&l]&f"
  prefix-help: "&c&l[&b&lUserShop Help&c&l]&f"
  prefix-error: "&4&l[&c&l오류&4&l]&f"

```