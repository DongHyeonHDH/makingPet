const items = [
  {id:"ribbon",name:"딸기 리본",type:"액세서리",icon:"🎀",price:70},
  {id:"hat",name:"새싹 모자",type:"액세서리",icon:"🌱",price:90},
  {id:"shirt",name:"구름 티셔츠",type:"상의",icon:"👕",price:120},
  {id:"boots",name:"노란 장화",type:"신발",icon:"🥾",price:80},
  {id:"rug",name:"폭신한 러그",type:"가구",icon:"🧶",price:110}
];
const listings = [
  {id:"a1",item:"ribbon",price:55,time:"23시간 58분"},
  {id:"a2",item:"hat",price:85,time:"11시간 20분"},
  {id:"a3",item:"shirt",price:105,time:"47시간 10분"},
  {id:"a4",item:"boots",price:75,time:"9시간 42분"},
  {id:"a5",item:"rug",price:95,time:"32시간 15분"}
];
state.market ??= {inventory:{},favorites:[],purchased:[],sales:[],completed:[]};
const market = state.market;
const itemFor = id => items.find(item => item.id === id);
const notice = message => { byId("status").textContent = message; };
const addItem = id => { market.inventory[id] = (market.inventory[id] || 0) + 1; };
const priceText = amount => amount.toLocaleString("ko-KR") + " 코인";
let tab = "search";
let selected = null;
let filters = {query:"",type:"",max:""};
let ascending = true;
let sortByPrice = false;
function purchase(item, listing) {
  const price = listing ? listing.price : item.price;
  if (state.coins < price || (listing && market.purchased.includes(listing.id))) return;
  state.coins -= price;
  addItem(item.id);
  if (listing) {
    market.purchased.push(listing.id);
    market.completed.push(listing);
    selected = null;
  }
  save();
  renderMarkets();
  notice(item.name + " 구매 완료");
}
function renderShop() {
  byId("shop-items").replaceChildren(...items.map(item => {
    const card = document.createElement("article");
    card.className = "card";
    const icon = document.createElement("span");
    icon.className = "shop-icon";
    icon.textContent = item.icon;
    const title = document.createElement("h2");
    title.textContent = item.name;
    const count = document.createElement("p");
    count.textContent = "보유 " + (market.inventory[item.id] || 0) + "개";
    const buy = document.createElement("button");
    buy.className = "action primary";
    buy.textContent = priceText(item.price);
    buy.disabled = state.coins < item.price;
    buy.onclick = () => purchase(item);
    card.append(icon,title,count,buy);
    return card;
  }));
}
function visibleListings() {
  const open = listings.filter(row => !market.purchased.includes(row.id));
  if (tab === "prices") return items.map(item => ({id:item.id,item:item.id,price:Math.min(...listings.filter(row => row.item === item.id).map(row => row.price)),time:"—"}));
  if (tab === "favorites") return open.filter(row => market.favorites.includes(row.id));
  if (tab === "sell") return market.sales;
  if (tab === "completed") return market.completed;
  return open.filter(row => {
    const item = itemFor(row.item);
    return item.name.includes(filters.query) && (!filters.type || item.type === filters.type) &&
      (filters.max === "" || row.price <= Number(filters.max));
  });
}
function renderAuction() {
  const rows = [...visibleListings()];
  if (sortByPrice) rows.sort((left,right) => (left.price - right.price) * (ascending ? 1 : -1));
  byId("filters").hidden = tab !== "search";
  byId("auction-grid").style.gridTemplateColumns = tab === "search" ? "" : "minmax(0,1fr)";
  byId("sell-form").hidden = tab !== "sell";
  byId("market-count").textContent = rows.length + "건";
  byId("market-empty").hidden = rows.length > 0;
  byId("market-rows").replaceChildren(...rows.map(listing => {
    const item = itemFor(listing.item);
    const row = document.createElement("tr");
    row.classList.toggle("selected", listing.id === selected);
    const choose = document.createElement("button");
    choose.type = "button";
    choose.textContent = item.icon + " " + item.name;
    choose.setAttribute("aria-pressed", String(listing.id === selected));
    choose.onclick = () => {
      selected = listing.id;
      renderAuction();
      byId("market-rows").querySelector('[aria-pressed="true"]')?.focus();
    };
    row.insertCell().append(choose);
    row.insertCell().textContent = priceText(listing.price);
    row.insertCell().textContent = priceText(listing.price);
    row.insertCell().textContent = listing.time;
    return row;
  }));
  const listing = rows.find(row => row.id === selected);
  const detail = byId("market-detail");
  detail.textContent = listing ? itemFor(listing.item).name + " · " + priceText(listing.price) : "목록에서 아이템을 선택해 주세요.";
  if (listing && (tab === "search" || tab === "favorites")) {
    const favorite = document.createElement("button");
    favorite.textContent = market.favorites.includes(listing.id) ? "찜 해제" : "찜하기";
    favorite.onclick = () => {
      market.favorites = market.favorites.includes(listing.id) ? market.favorites.filter(id => id !== listing.id) : [...market.favorites,listing.id];
      save();
      renderAuction();
    };
    const buy = document.createElement("button");
    buy.textContent = "구매";
    buy.disabled = state.coins < listing.price;
    buy.onclick = () => purchase(itemFor(listing.item),listing);
    detail.append(favorite,buy);
  }
  if (listing && tab === "sell") {
    const cancel = document.createElement("button");
    cancel.textContent = "등록 취소";
    cancel.onclick = () => {
      market.sales = market.sales.filter(row => row.id !== listing.id);
      addItem(listing.item);
      selected = null;
      save();
      renderMarkets();
      notice("판매 등록을 취소했어요.");
    };
    detail.append(cancel);
  }
}
function renderMarkets() {
  renderShop();
  const select = byId("sell-item");
  const previous = select.value;
  const owned = items.filter(item => market.inventory[item.id] > 0);
  select.replaceChildren(...owned.map(item => new Option(item.name + " (" + market.inventory[item.id] + "개)",item.id)));
  if (owned.some(item => item.id === previous)) select.value = previous;
  byId("sell-submit").disabled = !owned.length;
  byId("sell-empty").hidden = owned.length > 0;
  renderAuction();
}
document.querySelectorAll("[data-market-tab]").forEach(button => {
  button.onclick = () => {
    tab = button.dataset.marketTab;
    selected = null;
    document.querySelectorAll("[data-market-tab]").forEach(entry => entry.setAttribute("aria-selected",String(entry === button)));
    renderAuction();
  };
});
byId("filters").onsubmit = event => {
  event.preventDefault();
  filters = {query:byId("item-query").value.trim(),type:byId("item-type").value,max:byId("item-max").value};
  selected = null;
  renderAuction();
};
byId("filters").onreset = () => {
  filters = {query:"",type:"",max:""};
  selected = null;
  renderAuction();
};
byId("sort-price").onclick = () => {
  ascending = !sortByPrice || !ascending;
  sortByPrice = true;
  byId("sort-price").textContent = ascending ? "가격 ↑" : "가격 ↓";
  renderAuction();
};
byId("sell-form").onsubmit = event => {
  event.preventDefault();
  const id = byId("sell-item").value;
  const price = byId("sell-price").valueAsNumber;
  if (!market.inventory[id] || !Number.isSafeInteger(price) || price < 1) return;
  market.inventory[id] -= 1;
  market.sales.push({id:crypto.randomUUID(),item:id,price,time:"판매 중"});
  event.currentTarget.reset();
  save();
  renderMarkets();
  notice("판매 매물을 등록했어요.");
};
renderMarkets();


