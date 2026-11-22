const I='../../../YuhanCon/src/main/resources/static/images/';
const SHOWS=[
 {d:'12.20',t:'aespa LIVE TOUR – SYNK',p:'KSPO DOME',cat:'콘서트',img:I+'aespa.jpg',likes:128,dday:3},
 {d:'12.24',t:'BLACKPINK WORLD TOUR',p:'고척스카이돔',cat:'콘서트',img:I+'blackpink.jpg',likes:342,dday:7},
 {d:'12.27',t:'유한대 밴드부 정기공연',p:'유한대학교 대강당',cat:'학교공연',img:I+'concert.jpg',likes:41,dday:10},
 {d:'12.31',t:'싸이 흠뻑쇼 연말 스페셜',p:'잠실종합운동장',cat:'콘서트',img:I+'psy.jpg',likes:205,dday:14},
 {d:'01.04',t:'홍대 걷고싶은거리 버스킹',p:'홍대 걷고싶은거리',cat:'버스킹',img:I+'concert.jpg',likes:19,dday:18}];
const HEART='<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 21s-7.5-4.6-9.5-9.2C1 8.2 3.3 5 6.6 5c2 0 3.5 1.1 4.4 2.6C11.9 6.1 13.4 5 15.4 5 18.7 5 21 8.2 19.5 11.8 19.5 16.4 12 21 12 21z"/></svg>';
const TK=s=>`<a class="tk" href="#"><img src="${s.img}" alt=""><div class="body"><span class="cat">${s.cat}</span><div class="d num">${s.d}</div><div class="t">${s.t}</div><div class="p">${s.p}</div><div style="margin-top:10px"><span class="heart">${HEART}${s.likes}</span></div></div></a>`;
