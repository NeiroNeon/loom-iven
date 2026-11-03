/* Loom Iven — сайт: SVG-иконки, тема (тёмная/светая), язык (RU/EN), гамбургер.
   Русский текст живёт в HTML; словарь EN накладывается поверх, RU восстанавливает оригинал. */
(function () {
  'use strict';

  var COIN = '<svg class="mico coin" aria-hidden="true"><use href="#i-coin"/></svg>';

  /* ---------- SVG-спрайт ---------- */
  var SPRITE =
    '<svg xmlns="http://www.w3.org/2000/svg" style="display:none" aria-hidden="true">' +
    '<symbol id="i-loom" viewBox="0 0 32 32"><circle cx="16" cy="16" r="13" fill="none" stroke="currentColor" stroke-width="2"/><path d="M8 20c4-8 12-8 16 0M8 12c4 8 12 8 16 0" stroke="currentColor" stroke-width="2" fill="none"/></symbol>' +
    '<symbol id="i-coin" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="8.6"/><circle cx="12" cy="12" r="4.6"/></symbol>' +
    '<symbol id="i-compass" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="9"/><path d="M15.5 8.5 13.4 13.4 8.5 15.5l2.1-4.9z"/></symbol>' +
    '<symbol id="i-scroll" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M7 3h7l4 4v13a1 1 0 0 1-1 1H7a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1z"/><path d="M14 3v4h4"/><path d="M9.5 12h5M9.5 15.5h5"/></symbol>' +
    '<symbol id="i-users" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="9" cy="8" r="3.2"/><path d="M3.5 19c.6-3 2.8-4.6 5.5-4.6s4.9 1.6 5.5 4.6"/><circle cx="16.8" cy="9" r="2.6"/><path d="M16.4 14.6c2.4.2 4 1.6 4.6 4.4"/></symbol>' +
    '<symbol id="i-scale" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M12 4v16M8.5 20h7M6 7h12"/><path d="m6 7-2.8 5.6h5.6z"/><path d="m18 7-2.8 5.6h5.6z"/></symbol>' +
    '<symbol id="i-pad" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect x="2.5" y="7.5" width="19" height="10" rx="4.5"/><path d="M8 11v3M6.5 12.5h3"/><circle cx="15.3" cy="11.4" r="1.1" fill="currentColor" stroke="none"/><circle cx="17.7" cy="13.6" r="1.1" fill="currentColor" stroke="none"/></symbol>' +
    '<symbol id="i-cal" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect x="4" y="6" width="16" height="15" rx="2"/><path d="M4 10.5h16M8.5 3.5v4M15.5 3.5v4"/></symbol>' +
    '<symbol id="i-trophy" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M8 4h8v5a4 4 0 0 1-8 0z"/><path d="M8 5.5H5.5A3 3 0 0 0 8.4 9M16 5.5h2.5A3 3 0 0 1 15.6 9"/><path d="M12 13v4M8.5 20.5h7M12 17c-2 0-3.2 1.3-3.5 3.5M12 17c2 0 3.2 1.3 3.5 3.5"/></symbol>' +
    '<symbol id="i-ticket" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M6 6h12a2 2 0 0 1 2 2v2a2 2 0 0 0 0 4v2a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2v-2a2 2 0 0 0 0-4V8a2 2 0 0 1 2-2z"/><path d="M14 7v2M14 11v2M14 15v2"/></symbol>' +
    '<symbol id="i-key" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="7.5" cy="16.5" r="3.5"/><path d="m10.2 13.8 9.3-9.3M16.5 7.5l2.8 2.8M13.8 10.2l2 2"/></symbol>' +
    '<symbol id="i-cap" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="m12 4 10 5-10 5L2 9z"/><path d="M6.5 11.5V16c0 1.5 2.5 2.6 5.5 2.6s5.5-1.1 5.5-2.6v-4.5"/><path d="M22 9v5"/></symbol>' +
    '<symbol id="i-wheat" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M12 21V7"/><path d="M12 10c-2 0-3.5-1.5-3.5-4 2.5 0 3.5 1.5 3.5 4zM12 10c2 0 3.5-1.5 3.5-4-2.5 0-3.5 1.5-3.5 4z"/><path d="M12 15c-2 0-3.5-1.5-3.5-4 2.5 0 3.5 1.5 3.5 4zM12 15c2 0 3.5-1.5 3.5-4-2.5 0-3.5 1.5-3.5 4z"/></symbol>' +
    '<symbol id="i-box" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="m12 3 8 4.5v9L12 21l-8-4.5v-9z"/><path d="m4 7.5 8 4.5 8-4.5M12 12v9"/></symbol>' +
    '<symbol id="i-shield" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M12 3l8 3v6c0 4.5-3.2 7.8-8 9-4.8-1.2-8-4.5-8-9V6z"/></symbol>' +
    '<symbol id="i-moon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M20 14.5A8.5 8.5 0 0 1 9.5 4 8.5 8.5 0 1 0 20 14.5z"/></symbol>' +
    '<symbol id="i-sun" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="4"/><path d="M12 2.5v3M12 18.5v3M2.5 12h3M18.5 12h3M5 5l2.1 2.1M16.9 16.9 19 19M19 5l-2.1 2.1M7.1 16.9 5 19"/></symbol>' +
    '<symbol id="i-menu" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M4 7h16M4 12h16M4 17h16"/></symbol>' +
    '<symbol id="i-globe" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="9"/><path d="M3 12h18M12 3c2.8 2.6 4.2 5.6 4.2 9s-1.4 6.4-4.2 9c-2.8-2.6-4.2-5.6-4.2-9S9.2 5.6 12 3z"/></symbol>' +
    '<symbol id="i-user" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="8" r="4"/><path d="M4.5 20.5c1-4 4-6 7.5-6s6.5 2 7.5 6"/></symbol>' +
    '<symbol id="i-help" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="9"/><path d="M9.6 9.2A2.6 2.6 0 0 1 12 7.5c1.5 0 2.6 1 2.6 2.3 0 1.6-1.6 2-2.4 3-.2.3-.2.5-.2 1"/><circle cx="12" cy="16.8" r="1" fill="currentColor" stroke="none"/></symbol>' +
    '<symbol id="i-tg" viewBox="0 0 24 24" fill="currentColor" stroke="none"><path d="M11.944 0A12 12 0 0 0 0 12a12 12 0 0 0 12 12 12 12 0 0 0 12-12A12 12 0 0 0 12 0a12 12 0 0 0-.056 0zm4.962 7.224c.1-.002.321.023.465.14a.506.506 0 0 1 .171.325c.016.093.036.306.02.472-.18 1.898-.962 6.502-1.36 8.627-.168.9-.499 1.201-.82 1.23-.696.065-1.225-.46-1.9-.902-1.056-.693-1.653-1.124-2.678-1.8-1.185-.78-.417-1.21.258-1.91.177-.184 3.247-2.977 3.307-3.23.007-.032.014-.15-.056-.212s-.174-.041-.249-.024c-.106.024-1.793 1.14-5.061 3.345-.48.33-.913.49-1.302.48-.428-.008-1.252-.241-1.865-.44-.752-.245-1.349-.374-1.297-.789.027-.216.325-.437.893-.663 3.498-1.524 5.83-2.529 6.998-3.014 3.332-1.386 4.025-1.627 4.476-1.635z"/></symbol>' +
    '<symbol id="i-dc" viewBox="0 0 24 24" fill="currentColor" stroke="none"><path d="M20.317 4.37a19.79 19.79 0 0 0-4.885-1.515.074.074 0 0 0-.079.037c-.21.375-.444.864-.608 1.25a18.27 18.27 0 0 0-5.487 0 12.64 12.64 0 0 0-.617-1.25.077.077 0 0 0-.079-.037A19.736 19.736 0 0 0 3.677 4.37a.07.07 0 0 0-.032.027C.533 9.046-.32 13.58.099 18.058a.082.082 0 0 0 .031.056 19.9 19.9 0 0 0 5.993 3.03.078.078 0 0 0 .084-.028c.462-.63.874-1.295 1.226-1.994a.076.076 0 0 0-.041-.106 13.107 13.107 0 0 1-1.872-.892.077.077 0 0 1-.008-.128c.126-.094.252-.192.372-.291a.074.074 0 0 1 .077-.01c3.928 1.793 8.18 1.793 12.062 0a.074.074 0 0 1 .078.01c.12.098.246.198.373.292a.077.077 0 0 1-.006.127 12.3 12.3 0 0 1-1.873.891.077.077 0 0 0-.041.107c.36.698.772 1.362 1.225 1.993a.076.076 0 0 0 .084.028 19.84 19.84 0 0 0 6.002-3.03.077.077 0 0 0 .032-.054c.5-5.177-.838-9.674-3.549-13.66a.061.061 0 0 0-.031-.03zM8.02 15.33c-1.183 0-2.157-1.085-2.157-2.419 0-1.333.955-2.419 2.157-2.419 1.21 0 2.176 1.096 2.157 2.42 0 1.333-.956 2.418-2.157 2.418zm7.975 0c-1.183 0-2.157-1.085-2.157-2.419 0-1.333.955-2.419 2.157-2.419 1.21 0 2.176 1.096 2.157 2.42 0 1.333-.946 2.418-2.157 2.418z"/></symbol>' +
    '</svg>';

  /* ---------- EN-словарь (RU — в HTML) ---------- */
  var EN = {
    /* общий */
    'nav.home': 'Home', 'nav.mechanics': 'Mechanics', 'nav.quests': 'Quests & Orders',
    'nav.commands': 'Commands', 'nav.profile': 'Profile',
    'foot.about': 'Java + Bedrock Minecraft server: coins, progress and shared work.',
    'foot.pages': 'Pages', 'foot.docs': 'Documents',
    'foot.terms': 'Terms of Use', 'foot.privacy': 'Privacy Policy', 'foot.offer': 'Public Offer',
    'foot.community': 'Community', 'foot.copy': '© 2026 Loom Iven. Not affiliated with Mojang or Microsoft.',
    'foot.creator': 'Site built by <a href="https://t.me/SYNAPSE_of" target="_blank" rel="noopener">SYNAPSE</a> · <a href="https://discord.gg/2cuM7kpR" target="_blank" rel="noopener">Discord</a>',
    'i.prmentor': 'mentor',

    /* --- index --- */
    'i.title': 'Loom Iven — Minecraft server: survival & SMP',
    'i.desc': 'Loom Iven — a cross-platform Minecraft server (Java + Bedrock): coin economy, playtime ladder, auction, scheduled events, trader dailies and weekly orders.',
    'i.kicker': 'survival · smp · java + bedrock',
    'i.h1': 'Loom Iven — a server where progress<br>is woven hour by hour',
    'i.sub': 'A cross-platform Paper server: a coin economy, a playtime ladder, an auction, scheduled events, trader dailies and server-wide weekly orders.',
    'i.p1': '<b>Java</b> 26.x and older', 'i.p2': '<b>Bedrock</b> — phone and console',
    'i.p3': 'RU / <b>EN</b> your choice', 'i.p4': '<b>GrimAC</b> anticheat', 'i.p5': '<b>GriefPrevention</b> claims',
    'i.j1': 'How to join:', 'i.j2': 'address will appear once the server is hosted',
    'i.j3': 'Java — standard connection, Bedrock — port <b>19132</b>',
    'i.s1': 'What the server has',
    'i.c1t': 'economy', 'i.c1h': 'Coins instead of barter',
    'i.c1p': 'Its own currency, not tied to diamonds. Earn at events, on the auction, from trader dailies and order contributions. Prices are tuned so rewards sit slightly above market value.',
    'i.c2t': 'progress', 'i.c2h': 'Playtime ladder',
    'i.c2p': 'Novice → Resident → Keeper → Legend by hours played. Ranks unlock the auction, Keeper events and trading quarter plots. Anti-AFK keeps playtime honest.',
    'i.c3t': 'daily', 'i.c3h': 'Things to do every day',
    'i.c3p': 'Trader Ivlia’s dailies — up to 10 orders a day, 36 achievements in 10 categories, a 30-day season pass and cosmetics for hours played.',
    'i.c4t': 'together', 'i.c4h': 'Shared goals',
    'i.c4p': 'The “Common Fund” — a coin pool with a guaranteed payout of 105% to 150% of each contribution. The weekly order — the whole server brings wheat, the pool is split by contribution share.',
    'i.c5t': 'trading', 'i.c5h': 'Auction',
    'i.c5p': 'Bids step up by 5%, lots live for 48 hours, late bids extend the timer. Outbid players are refunded instantly; the winner pays a 5% fee.',
    'i.c6t': 'crossplay', 'i.c6h': 'Java and Bedrock together',
    'i.c6p': 'New and old Java clients (ViaVersion/ViaBackwards) and Bedrock clients (Geyser + Floodgate) play in one world. You can join from a phone.',
    'i.prh': 'What a profile looks like',
    'i.prp': 'Your profile builds itself: hours, rank and coins are counted by the server. The main page shows the same card as the <a href="profile.html">profile page</a>.',
    'i.prnote': '143 h — 217 h to go until the Legend rank',
    'i.prs1': 'coins', 'i.prs2': 'achievements', 'i.prs3': 'pass day', 'i.prs4': 'students',
    'i.lh': 'Playtime ladder',
    'i.lp': 'Only live play counts: 10 minutes without movement turns off event points, and 6 hours of AFK stop the hour counter.',
    'i.l1h': '0 hours', 'i.l1n': 'Novice', 'i.l1p': 'all core gameplay',
    'i.l2h': '24 hours', 'i.l2n': 'Resident', 'i.l2p': 'auction unlocks (/ah)',
    'i.l3h': '120 hours', 'i.l3n': 'Keeper', 'i.l3p': 'Keeper events, quarter plots',
    'i.l4h': '360 hours', 'i.l4n': 'Legend', 'i.l4p': 'cosmetic keys for every 20 h',
    'i.lnote': 'Above Legend — cosmetic titles: Veteran (168 h, skips night for the whole server), Elder (336 h, mentoring), Virtuoso (504 h), Icon (672 h).',
    'i.fh': 'Frequent questions',
    'i.fq1': 'Who is trader Ivlia and where do I find her?',
    'i.fa1': 'He is an NPC villager who posts 10 new orders every day: bring resources, potions or valuable items — get coins with a markup. Right-click the NPC to open the order board. Command: <code>/квесты</code> (or <code>/quests</code>).',
    'i.fq2': 'What is the “Common Fund” and what do I get?',
    'i.fa2': 'The admin announces a target and players contribute coins. The payout pool is <b>120% of everything collected</b> (the quarter treasury adds the extra 20%), and every participant receives <b>105% to 150% of their own contribution</b>. If less than half the target is collected, everything is refunded — you lose nothing.',
    'i.fq3': 'How is the “weekly order” different?',
    'i.fa3': 'The weekly order is about items: every Monday the server gets a target (say, 2,000,000 wheat and a 20,000-coin pool). The pool is split strictly proportionally to contributions. Details in the <a href="quests.html">“Quests & Orders”</a> section.',
    'i.fq4': 'Do I need to donate to play comfortably?',
    'i.fa4': 'No. Every gameplay mechanic is free. A paid track exists only in the season pass (25,000 coins or rubles via the shop), and its unique cosmetics move to the free track next season.',
    'i.fq5': 'Which version can I join with?',
    'i.fa5': 'Java — current versions (Paper 26.3) and older ones via ViaVersion/ViaBackwards. Bedrock — any current client via Geyser, port 19132.',
    'i.fq6': 'How do I claim territory?',
    'i.fa6': 'Select two corners of your plot with a golden shovel — the claim is created automatically (GriefPrevention). Building tools — WorldEdit for trusted work, protection — WorldGuard.',

    /* --- mechanics --- */
    'm.title': 'Mechanics — Loom Iven',
    'm.desc': 'A full description of Loom Iven mechanics: economy, playtime ladder, auction, events, achievements, season pass, keys, mentors, trading quarter, Common Fund, dailies and the weekly order.',
    'm.h1': 'Server mechanics',
    'm.sub': 'Everything that runs on Loom Iven. Commands are on a <a href="commands.html">separate page</a>; trader dailies and the weekly order are covered in a <a href="quests.html">dedicated section</a>.',
    'm.a1': 'Economy & language', 'm.a2': 'Playtime ladder', 'm.a3': 'Auction', 'm.a4': 'Scheduled events',
    'm.a5': '36 achievements', 'm.a6': 'Season pass', 'm.a7': 'Keys & cosmetics', 'm.a8': 'Mentors & hall of fame',
    'm.a9': 'Trading quarter', 'm.a10': 'Common Fund', 'm.a11': 'Trader Ivlia (dailies)', 'm.a12': 'Weekly order',
    'm.a13': 'Menu, profile and HUD',
    'm.eco.h': 'Economy & language',
    'm.eco.p1': 'The server currency is <b>coins</b>. Balance: <code>/монеты</code>. Earnings: events, auction, dailies, contributions to funds and orders, achievements, mentoring. Spending: auction, quarter plots (3,000), the pass donate track (25,000), contributions.',
    'm.eco.p2': 'Every player picks an interface language: <code>/язык ru</code> or <code>/язык en</code> (Russian/English aliases work too). All messages, GUIs and rewards are shown to you personally in your language.',
    'm.gui.h': 'Menu, profile and HUD',
    'm.gui.p1': 'The <code>/меню</code> command puts every server feature on one screen: profile, auction, Ivlia\u2019s dailies, community fund, achievements, pass, cosmetics, home and spawn. Java players get a chest GUI, Bedrock players get a native form; every item is labelled with what it does, the statistics behind it and what a click will do.',
    'm.gui.p2': 'The profile (<code>/профиль</code>) shows everything the server knows about you: hours and progress to the next rank, coins, active cosmetics, achievements, pass day, weekly order and fund contributions, auction lots and payouts. On the right side of the screen there is a scoreboard with the server logo: online, coins, rank, fund and order progress, daily counter. The same logo lives in the tab-list header and footer.',
    'm.ladder.h': 'Playtime ladder',
    'm.l1h': '0 h', 'm.l1n': 'Novice', 'm.l1p': 'core game',
    'm.l2h': '24 h', 'm.l2n': 'Resident', 'm.l2p': 'auction',
    'm.l3h': '120 h', 'm.l3n': 'Keeper', 'm.l3p': 'events+, plots',
    'm.l4h': '360 h', 'm.l4n': 'Legend', 'm.l4p': 'keys for hours',
    'm.ladder.p1': 'Progress: <code>/часы</code>, top-10: <code>/часы топ</code> (English aliases: /hours). Ranks sync to permissions (LuckPerms) automatically and sort the tab list. Anti-AFK: 10 idle minutes — no event points; 6 hours — the hour counter freezes.',
    'm.ladder.p2': 'Cosmetic titles for extra hours: <b>Veteran</b> 168 h (lies in a bed — morning comes for the whole server), <b>Elder</b> 336 h (mentoring), <b>Virtuoso</b> 504 h, <b>Icon</b> 672 h.',
    'm.auc.h': 'Auction (from Resident)',
    'm.auc.l1': '<b>Listing:</b> <code>/ah sell &lt;price&gt; [buyout]</code> — the item in your hand, up to 5 active lots.',
    'm.auc.l2': '<b>Bidding:</b> the step is 5% of the current price (minimum 5 coins). Outbid players are refunded instantly.',
    'm.auc.l3': '<b>Timer:</b> a lot lives for 48 hours; a bid in the last 5 minutes extends the auction by 5 minutes, up to 3 extensions — last-second sniping will not work.',
    'm.auc.l4': '<b>Fee:</b> 5% of the winning bid (minimum 10 coins) stays with the server.',
    'm.auc.l5': '<b>Delivery:</b> money and items wait in <code>/ah claim</code>.',
    'm.ev.h': 'Scheduled events',
    'm.ev.p1': 'Each event day starts at 18:00 and lasts 3 hours. Points count only for live play (anti-AFK).',
    'm.ev.th1': 'Day', 'm.ev.th2': 'Event', 'm.ev.th3': 'Group', 'm.ev.th4': 'What to do',
    'm.ev.r1c1': 'Tuesday', 'm.ev.r1c2': 'Double drop', 'm.ev.gnew': 'novices', 'm.ev.r1c4': 'break blocks — they drop twice',
    'm.ev.r2c1': 'Wednesday', 'm.ev.r2c2': 'Iron run', 'm.ev.r2c4': 'mine ores for points',
    'm.ev.r3c1': 'Thursday', 'm.ev.r3c2': '—', 'm.ev.r3c4': 'rest day',
    'm.ev.r4c1': 'Friday', 'm.ev.r4c2': 'Big hunt', 'm.ev.gkeep': 'Keeper+', 'm.ev.r4c4': 'kill monsters for points',
    'm.ev.r5c1': 'Saturday', 'm.ev.r5c2': 'Boss battle', 'm.ev.r5c4': 'bosses give ×10 points',
    'm.ev.r6c1': 'Sunday', 'm.ev.r6c2': 'Harvest', 'm.ev.r6c4': 'gather crops',
    'm.ev.p2': 'Top-3 rewards are <b>split by group</b> — a novice never competes with a veteran. Top-1 gets a cosmetic key. Status and schedule: <code>/ивент</code> (or /events).',
    'm.ach.h': '36 achievements in 10 categories',
    'm.ach.p1': 'Mining, building, hunting, farming, fishing, trading, social, collecting, exploring, special. Each achievement pays coins and progress. Progress and categories: <code>/ачивки</code> (or /ach).',
    'm.pass.h': 'Season pass',
    'm.pass.l1': 'A season is <b>30 days</b> with a reward for each: <code>/пропуск</code> (or /pass).',
    'm.pass.l2': 'The donate track is <b>25,000 coins</b> (or rubles via the shop), unique cosmetics from day 15.',
    'm.pass.l3': '<b>Fair migration:</b> next season the previous donate-track cosmetics move to the free track on day 25 and later.',
    'm.keys.h': 'Keys & cosmetics',
    'm.keys.p1': 'After Legend (360 h) every <b>20 hours played</b> grants a cosmetic key. Open: <code>/ключи open</code>, wear cosmetics: <code>/ключи wear &lt;name&gt;</code>. Some cosmetics also come from events, achievements and the pass.',
    'm.men.h': 'Mentors & hall of fame',
    'm.men.p1': 'An Elder (336 h) receives a <b>reusable personal code</b>: <code>/ментор</code>. A novice enters <code>/ментор код &lt;code&gt;</code> to confirm the mentorship. Mentors earn rewards for their students’ progress; profile data is stored in a format ready for the future website. Hall of fame: <code>/слава</code> — tops by hours, coins, achievements and mentoring.',
    'm.qu.h': 'Trading quarter',
    'm.qu.p1': 'The admin builds the district: <code>/квартал build all</code> (requires <code>quarter.enabled: true</code> in the config). A Keeper (120 h) buys a free plot for <b>3,000 coins</b>: <code>/участок</code> — your shop among the others, in one place.',
    'm.fund.h': 'Common Fund',
    'm.fund.p1': 'A crowdfunding event: the server collects <b>coins</b> toward a target. Payouts follow the <b>“Break-even participant”</b> rule — every contribution comes back with a profit.',
    'm.fund.l1': 'Start: <code>/сбор start &lt;target&gt; [hours]</code> (admin) — a boss-bar appears for everyone, milestones 25/50/75% are announced in chat.',
    'm.fund.l2': 'Contribute: <code>/сбор вклад &lt;amount&gt;</code>; status: <code>/сбор</code>; top: <code>/сбор топ</code>.',
    'm.fund.l3': 'The payout pool is <b>120% of everything collected</b>: the quarter treasury adds the extra 20% through co-financing, subsidies and the project’s commercial margin.',
    'm.fund.l4': 'Each participant’s payout is clamped to <b>105–150% of their own contribution</b>: the 105% floor guarantees a profit, the 150% ceiling protects the treasury. Inside the range the split stays proportional.',
    'm.fund.l5': 'If <b>less than 50% of the target</b> is collected by the deadline — full refund (100%): you lose nothing. An admin cancel also refunds everything.',
    'm.fund.exh': 'Example:',
    'm.fund.ex1': '<br>• Target 100,000 coins, collected 100,000 → payout pool <b>120,000</b>.<br>• Mira contributed 10,000 (10% of all contributions) → her base share is 12,000 → <span class="ok">+12,000 coins (120%)</span>.<br>• If only 60% of the target were collected, the pool would still cover at least <b>105%</b> of every contribution.',
    'm.tr.h': 'Trader Ivlia — dailies',
    'm.tr.p1': 'An NPC villager at spawn posts <b>10 orders</b> from a pool of 59 every day: resources, potions, valuable items. The reward is the item’s market price × a 1.25–1.4 bonus, so an order always beats a plain sale. Each order is handed in once a day, max 10/day. Click the NPC for the GUI board, or use <code>/квесты сдать &lt;id&gt;</code> (or /quests).',
    'm.tr.p2': 'Sample orders: 1,024 wheat → 110 coins; 192 iron ingots → 840; 32 diamonds → 1,165; 6 healing II potions → 325; 4 enchanted books → 600. The full table is in the <a href="quests.html">“Quests & Orders”</a> section.',
    'm.or.h': 'Weekly order',
    'm.or.p1': 'Every Monday the whole server gets a shared goal: for example <b>2,000,000 wheat</b> and a <b>20,000-coin</b> pool. Everyone brings items (<code>/заказ сдать &lt;amount|all&gt;</code>), progress is visible to all (boss-bar + 25/50/75% milestones). When the target is reached (or the week ends) the pool is split <b>strictly proportionally to contributions</b>: bring 1% of everything — get 1% of the pool.',
    'm.or.p2': 'The math with examples is in the <a href="quests.html">“Quests & Orders”</a> section.',

    /* --- quests --- */
    'q.title': 'Quests & Orders — Loom Iven',
    'q.desc': 'Trader Ivlia’s dailies (up to 10 a day) and the global weekly order on Loom Iven: the order pool, reward balance, worked examples of the prize pool.',
    'q.h1': 'Quests & orders',
    'q.sub': 'Two systems keep the economy moving: personal <b>trader dailies</b> and the <b>server-wide weekly order</b>.',
    'q.tr.h': 'Trader Ivlia: dailies',
    'q.tr.h3a': 'How it works',
    'q.tr.l1': 'At spawn stands an NPC — <b>Ivlia, the wandering trader</b>. Right-click opens the order board in a GUI.',
    'q.tr.l2': 'Every day at 00:00 the board gets <b>10 new orders</b> from a pool of 59 — the same list for all players.',
    'q.tr.l3': 'Each order asks you to bring items: resources, potions or rare items. Clicking an order = handing it in: items are taken, coins arrive instantly.',
    'q.tr.l4': 'Each order is handed in <b>once a day</b>, and no more than <b>10 orders a day</b> per player — that is the daily limit.',
    'q.tr.l5': 'GUI alternatives: <code>/квесты список</code> — a text list, <code>/квесты сдать &lt;id&gt;</code> — hand in by id, <code>/квесты лог</code> — what you already handed in.',
    'q.tr.h3b': 'Why orders are worth taking',
    'q.tr.p1': 'The balance follows a formula:',
    'q.tr.f': 'reward = amount × item market price × bonus (1.25–1.4)',
    'q.tr.p2': 'The bonus is the “delivery fee”: handing in an order pays 25–40% more than the goods alone are worth. That is why a quest beats simply selling or using the resources. Harder orders (brewing potions, hauling from the End/Nether) get a bigger bonus than bulk farming.',
    'q.tr.h3c': 'Categories and sample orders',
    'q.th1': 'Category', 'q.th2': 'Order', 'q.th3': 'Reward', 'q.th4': 'Balance logic',
    'q.cat1': 'resources', 'q.cat2': 'potions', 'q.cat3': 'items',
    'q.r1': '1,024 wheat', 'q.v1': '110 ' + COIN, 'q.d1': 'bulk, cheap, +35% bonus',
    'q.r2': '768 carrots', 'q.v2': '90 ' + COIN, 'q.d2': 'farming',
    'q.r3': '512 sugar cane', 'q.v3': '95 ' + COIN, 'q.d3': 'farm + combining',
    'q.r4': '192 iron ingots', 'q.v4': '840 ' + COIN, 'q.d4': 'ore is worth more than food',
    'q.r5': '32 diamonds', 'q.v5': '1,165 ' + COIN, 'q.d5': 'rare ore — big bonus',
    'q.r6': '32 ender pearls', 'q.v6': '500 ' + COIN, 'q.d6': 'risk (End, endermen)',
    'q.r7': '24 blaze rods', 'q.v7': '500 ' + COIN, 'q.d7': 'risk (Nether fortress)',
    'q.r8': '16 ghast tears', 'q.v8': '475 ' + COIN, 'q.d8': 'high risk',
    'q.r9': '128 honeycombs', 'q.v9': '90 ' + COIN, 'q.d9': 'beekeeping',
    'q.r10': '1,024 snowballs', 'q.v10': '65 ' + COIN, 'q.d10': 'a joke “snow order”',
    'q.r11': '6 healing II potions', 'q.v11': '325 ' + COIN, 'q.d11': 'brewing: tear + melon',
    'q.r12': '6 strength II potions', 'q.v12': '350 ' + COIN, 'q.d12': 'brewing: gunpowder + torch + stairs',
    'q.r13': '8 fire resistance potions', 'q.v13': '380 ' + COIN, 'q.d13': 'needs magma cream',
    'q.r14': '12 night vision potions', 'q.v14': '320 ' + COIN, 'q.d14': 'carrot + gold nuggets',
    'q.r15': '4 invisibility potions', 'q.v15': '300 ' + COIN, 'q.d15': 'needs rabbit’s foot',
    'q.r16': '4 enchanted books', 'q.v16': '600 ' + COIN, 'q.d16': 'any enchantments',
    'q.r17': '16 golden apples', 'q.v17': '730 ' + COIN, 'q.d17': '8 ingots × 8 each',
    'q.r18': '128 golden carrots', 'q.v18': '915 ' + COIN, 'q.d18': 'horse food, takes a while',
    'q.r19': '64 bottles o’ enchanting', 'q.v19': '540 ' + COIN, 'q.d19': 'cleric villagers only',
    'q.r20': '2 hearts of the sea', 'q.v20': '810 ' + COIN, 'q.d20': 'from treasure maps',
    'q.r21': '4 lodestones', 'q.v21': '470 ' + COIN, 'q.d21': 'a tricky craft',
    'q.note1': 'The full pool — 59 orders (34 resources, 8 potions, 17 items) — lives in the LoomTrader config and sources. An admin can regenerate the board on the fly: <code>/торговец реролл</code>, and change the reward multiplier (<code>daily.reward-multiplier</code>).',
    'q.tr.h3d': 'Interactions',
    'q.tr.i1': '<b>Board ↔ inventory:</b> each order’s lore shows “You have: N / need M” — no need to open your inventory.',
    'q.tr.i2': '<b>Dailies ↔ events:</b> the “1,024 wheat” order pairs nicely with the Harvest event (Sunday), and “iron” with Iron Run (Wednesday). The event gives points, the daily gives coins — for the same resource.',
    'q.tr.i3': '<b>Dailies ↔ auction:</b> daily earnings are a safe source of coins for your first bids.',
    'q.tr.i4': '<b>Language:</b> order names, categories and the whole board are shown in the player’s language (RU/EN).',
    'q.or.h': 'Global weekly order',
    'q.or.h3a': 'The idea',
    'q.or.p1': 'Every Monday at 00:00 the server automatically receives <b>one big goal for everyone</b> (a rotation of 5 orders; the admin can launch a custom one any time).',
    'q.thw1': 'Week', 'q.thw2': 'Target', 'q.thw3': 'Prize pool',
    'q.w1': 'wheat', 'q.wv1': '20,000 ' + COIN,
    'q.w2': 'carrots', 'q.wv2': '18,000 ' + COIN,
    'q.w3': 'sugar cane', 'q.wv3': '18,000 ' + COIN,
    'q.w4': 'coal', 'q.wv4': '25,000 ' + COIN,
    'q.w5': 'iron ingots', 'q.wv5': '30,000 ' + COIN,
    'q.or.h3b': 'How to take part',
    'q.or.l1': 'Hand in: <code>/заказ сдать &lt;amount&gt;</code> — items leave your inventory, the contribution is recorded. <code>/заказ сдать все</code> hands in everything at once.',
    'q.or.l2': 'Progress is visible to everyone: a <b>boss-bar</b> above the hotbar + announcements at <b>25 / 50 / 75%</b> milestones.',
    'q.or.l3': 'Status: <code>/заказ</code> — target, collected, your contribution, your % and payout forecast. Leaderboard: <code>/заказ топ</code>.',
    'q.or.l4': 'Once the target is reached the order finishes <b>immediately</b>, without waiting for the end of the week.',
    'q.or.l5': 'If the deadline passes before the target — the pool is still split by contribution shares: effort is never wasted.',
    'q.or.h3c': 'How the pool is split',
    'q.or.f': 'player payout = pool × (their contribution / total of all contributions)',
    'q.or.p2': 'Everyone’s share is <b>strictly proportional</b> — the more wheat you brought, the more coins you get. The minimum payout is 1 coin for any positive contribution (even 2 wheat don’t “burn”).',
    'q.or.exh': 'Example (wheat, pool 20,000 ' + COIN + '):',
    'q.or.ex': '<br>• Stasis brought <b>200,000 wheat</b> out of 1,000,000 collected → 20% share → <span class="ok">+4,000 ' + COIN + '</span><br>• Mira brought <b>50,000 wheat</b> → 5% share → <span class="ok">+1,000 ' + COIN + '</span><br>• Timosha brought <b>2 wheat</b> → 0.0002% share → <span class="ok">+1 ' + COIN + '</span> (the minimum)<br>• Total paid out: exactly 20,000 ' + COIN + ' — the pool is neither “printed” nor “burned”.',
    'q.or.exn': 'Your % of the pool depends on how much the server as a whole brings: the bigger the total, the smaller your slice — so it pays to contribute early and steadily all week.',
    'q.or.h3d': 'Interactions',
    'q.or.i1': '<b>Order ↔ events:</b> wheat and cane weeks line up with Harvest (Sunday) — event points and an order contribution from the same crop.',
    'q.or.i2': '<b>Order ↔ Common Fund:</b> different mechanics. The “Fund” collects coins (the admin launches it for any occasion; payouts follow the “Break-even participant” rule — 105–150% of your contribution, pool = 120% of collected), the “order” is about items and repeats automatically every week.',
    'q.or.i3': '<b>Order ↔ auction:</b> after payouts the auction traditionally sees more active bids — the pool flows into the economy.',
    'q.or.i4': '<b>Admin control:</b> <code>/заказ start &lt;item&gt; &lt;target&gt; &lt;pool&gt; [days]</code>, <code>/заказ finish</code>, <code>/заказ cancel</code> (no payouts).',
    'q.f.h': 'Frequent questions',
    'q.f.q1': 'Can I hand in quests as a novice (0 hours)?',
    'q.f.a1': 'Yes — dailies and the weekly order are open to everyone from the first login. It is deliberate design: a novice gets a safe source of coins right away, independent of the auction (which opens at 24 hours).',
    'q.f.q2': 'What if my stack has named items (custom swords, named ingots)?',
    'q.f.a2': 'Regular orders accept items by type, so named ingots work too. Potions, however, are accepted strictly by potion type, and enchanted books only if genuinely enchanted.',
    'q.f.q3': 'What if two players take turns to “inflate” the pool?',
    'q.f.a3': 'You cannot inflate it: the pool is fixed and does not grow with the number of contributors. Redistributing it “among friends” only works by actually bringing more items — which is an honest share.',
    'q.f.q4': 'Can items be returned if an order is cancelled?',
    'q.f.a4': 'In the weekly order the items are already spent on the target, so an admin cancel pays nothing back — cancellation is reserved for exceptional cases. In the “Common Fund” (coins) the “Break-even participant” rule applies: if less than 50% of the target was collected — a 100% refund; an admin cancel refunds too.',

    /* --- commands --- */
    'cmd.title': 'Commands — Loom Iven',
    'cmd.desc': 'The complete command reference for Loom Iven: GUI menu and profile, HUD scoreboard, economy, hours, auction, events, achievements, pass, keys, mentors, quarter, Common Fund, trader dailies and the weekly order.',
    'cmd.h1': 'Command reference',
    'cmd.sub': 'Every command answers to both Russian and English aliases. The reply language is the one you picked (<code>/язык ru|en</code> or <code>/lang</code>).',
    'cmd.th1': 'Command', 'cmd.th2': 'Who', 'cmd.th3': 'Description', 'cmd.all': 'all', 'cmd.admin': 'admin',
    'cmd.eco.h': 'Economy & core (LoomCore)',
    'cmd.eco.r1': 'coin balance (aliases: /coins, /money, /bal; Russian /монеты)',
    'cmd.eco.r2': 'personal interface language (aliases: /lang, /язык)',
    'cmd.eco.r3': 'manage coins',
    'cmd.eco.r4': 'reload configs',
    'cmd.eco.r5': 'GUI menu with every server feature (aliases: /menu, /гуи); Bedrock gets a native form',
    'cmd.eco.r6': 'player profile with full statistics (alias: /profile); Bedrock gets a form',
    'cmd.gui.h': 'GUI & HUD (LoomCore 1.5)',
    'cmd.gui.p': 'Every action that fits an interface is done through a GUI: Java players get classic chests, Bedrock players get native forms. Each item in a GUI carries a description, statistics and a click hint.',
    'cmd.gui.l1': '<b>/меню</b> — one screen: profile, auction, Ivlia\u2019s dailies, community fund, achievements, season pass, cosmetics, home/spawn, command list (admins get their own section).',
    'cmd.gui.l2': '<b>/профиль</b> — hours, rank and progress to the next one, coins, cosmetics, achievements, pass, order and fund contributions, auction lots and payouts, ping and first join.',
    'cmd.gui.l3': '<b>Right-side scoreboard</b> — the server logo on top, online, coins, hours and rank, fund and weekly order progress, daily quest counter.',
    'cmd.gui.l4': '<b>Tab list</b> — the same logo in the header and footer, online count and command hints; players sorted by rank.',
    'cmd.gui.l5': '<b>Bedrock (Geyser/Floodgate)</b> — menu, profile, fund and pass open as forms with amount input; the auction and order board work through the same chests.',
    'cmd.hrs.h': 'Hours (LoomPlaytimeRanks)',
    'cmd.hrs.r1': 'hours, rank, progress to the next one (aliases: /hours, /playtime, /часы)',
    'cmd.hrs.r2': 'top-10 by hours played',
    'cmd.ah.h': 'Auction (LoomAuction) — Resident+',
    'cmd.ah.r1': 'auction showcase in a GUI (aliases: /auction, /аукцион); left click — bid/buyout',
    'cmd.ah.r2': 'list the item in your hand (up to 5 lots)',
    'cmd.ah.r3': 'my lots (right click — cancel while there are no bids)',
    'cmd.ah.r4': 'collect payouts and purchased items',
    'cmd.ah.n': 'Rules: bid step 5% (min 5), lot lives 48 h, a bid in the last 5 minutes extends by 5 min (max 3), winner’s fee 5% (min 10), instant refunds for outbid players.',
    'cmd.ev.h': 'Events & progress (LoomProgress)',
    'cmd.ev.r1': 'current event, points, schedule (aliases: /events, /ивенты)',
    'cmd.ev.r2': '36 achievements in 10 categories (alias: /ach)',
    'cmd.ev.r3': 'season pass: days, rewards (aliases: /pass, /seasonpass)',
    'cmd.ev.r4': 'buy the donate track (25,000 coins)',
    'cmd.ev.r5': 'claim the day-15 reward',
    'cmd.ev.r6': 'cosmetic keys (alias: /keys)',
    'cmd.ev.r7': 'open a key · wear cosmetics',
    'cmd.ev.r8': 'your mentor code (aliases: /mentor, /наставник)',
    'cmd.ev.r9': 'a student confirms the mentorship',
    'cmd.ev.r10': 'hall of fame: tops by hours, coins, achievements, mentors (alias: /fame)',
    'cmd.ev.r11': 'trading quarter info (alias: /quarter)',
    'cmd.ev.r12': 'admin: build the quarter (needs quarter.enabled)',
    'cmd.ev.r13': 'claim a plot — Keeper+, 3,000 coins (alias: /plot)',
    'cmd.fund.h': 'Common Fund (LoomProgress)',
    'cmd.fund.r1': 'status: target, collected, %, time left, your contribution',
    'cmd.fund.r2': 'contribute coins (min 50)',
    'cmd.fund.r3': 'contributor leaderboard',
    'cmd.fund.r4': 'start (boss-bar, 25/50/75% milestones)',
    'cmd.fund.r5': 'cancel with a full refund',
    'cmd.fund.n': 'The “Break-even participant” rule: payout pool = <b>120% of everything collected</b> (the quarter treasury adds 20%), each participant receives <b>105–150% of their contribution</b>. Under 50% of the target — full refund.',
    'cmd.tr.h': 'Trader dailies (LoomTrader)',
    'cmd.tr.r1': 'open Ivlia’s order board in a GUI (aliases: /quests, /dailies)',
    'cmd.tr.r2': 'hand in an order by id (e.g. /квесты сдать wheat)',
    'cmd.tr.r3': 'the board as text in chat',
    'cmd.tr.r4': 'what you already handed in today',
    'cmd.tr.r5': 'admin: place the NPC at your position (or spawn x y z [yaw] [world])',
    'cmd.tr.r6': 'admin: remove the NPC',
    'cmd.tr.r7': 'admin: regenerate the board (10 fresh orders)',
    'cmd.tr.r8': 'admin: reload the config',
    'cmd.tr.n': 'The board refreshes every day at 00:00 (10 orders from a pool of 59). Each order once a day, max 10 handed-in orders per day. Click the NPC = open the board.',
    'cmd.or.h': 'Weekly order (LoomTrader)',
    'cmd.or.r1': 'status: target, collected, your contribution, your %, payout forecast',
    'cmd.or.r2': 'contribute items from your inventory',
    'cmd.or.r3': 'top-10 contributors',
    'cmd.or.r4': 'a custom order instead of the rotation',
    'cmd.or.r5': 'finish now and split the pool by shares',
    'cmd.or.r6': 'cancel without payouts',
    'cmd.or.n': 'Monday rotation: wheat 2,000,000 (20,000 coins) → carrots 1,500,000 (18,000) → cane 1,200,000 (18,000) → coal 500,000 (25,000) → iron 200,000 (30,000).',
    'cmd.oth.h': 'Misc',
    'cmd.oth.th1': 'Action',
    'cmd.oth.r1': 'Night skip', 'cmd.oth.d1': 'a Veteran (168 h+) lies in a bed — morning comes for the whole world',
    'cmd.oth.r2': 'Anti-AFK', 'cmd.oth.d2': '10 min without movement — no event points; 6 h — the hour counter stops',
    'cmd.oth.r3': 'Keys for hours', 'cmd.oth.d3': '1 key for every 20 h after Legend (360 h)',

    /* --- profile --- */
    'pr.title': 'Profile — Loom Iven',
    'pr.desc': 'A Loom Iven player profile: hours, activity-ladder rank, coins, achievements, season pass, mentoring.',
    'pr.h1': 'Player profile',
    'pr.sub': 'Your profile is your on-server record: hours, rank, coins and achievements. Below is a sample profile; in game the live numbers are at <code>/часы</code>, <code>/монеты</code> and <code>/ачивки</code>.',
    'pr.note': '143 h — 217 h to go until the Legend rank',
    'pr.s5': 'quarter plot',
    'pr.rh': 'The profile rank = the plate in game',
    'pr.rp': 'In game the profile rank shows as a <b>plate before the nickname</b> — in chat, above the head and in the tab list. The plate is granted by the playtime ladder:',
    'pr.rn': 'Administration has its own plates ([Admin], [Moder]), and so do mentors ([Mentor]). The nickname itself stays plain white — only the privilege name is coloured.',
    'pr.ch': 'What else the profile includes',
    'pr.c1h': 'Achievements',
    'pr.c1p': '36 achievements in 10 categories: mining, building, hunting, farming, fishing, trading and more. Each pays coins, some pay cosmetic keys.',
    'pr.c2h': 'Season pass',
    'pr.c2p': '30 days with a reward for each: coins, keys, cosmetics. The donate track is optional — its unique items move to the free track next season.',
    'pr.c3h': 'Mentoring',
    'pr.c3p': 'An Elder (336 h) gets a personal mentor code. Students confirm with the code; the mentor earns coins and keys for their progress.',
    'pr.demonote': 'The data on this page is a design sample. Public profiles with real stats will appear once the server is hosted.',

    /* --- terms --- */
    't.title': 'Terms of Use — Loom Iven',
    't.desc': 'Terms of use for the Loom Iven website and game server: account, game rules, punishments, user content, liability.',
    't.h1': 'Terms of Use',
    't.meta': 'Edition of November 1, 2026 · the Loom Iven project',
    't.s1h': '1. General',
    't.s1p1': 'This agreement describes the rules for using the Loom Iven website and game server (the “Project”). By joining the server or using the site you accept the terms below. If any clause does not suit you — please do not use the Project.',
    't.s1p2': 'The Project is not affiliated with Mojang Studios or Microsoft and is not an official Minecraft product. Playing Minecraft is also governed by the Mojang EULA: <a href="https://www.minecraft.net/ru-ru/eula" target="_blank" rel="noopener">minecraft.net/eula</a>.',
    't.s2h': '2. Account',
    't.s2l1': 'The account owner is responsible for everything done on the server. Keep your Minecraft credentials and Discord link secure.',
    't.s2l2': 'One player — one account. Evading a ban with an alt account is forbidden and leads to both accounts being blocked.',
    't.s3h': '3. Game rules',
    't.s3l1': 'Cheats, X-ray, forbidden modifications and exploiting server bugs for gain (dupes, bypassing mechanic limits) are forbidden.',
    't.s3l2': 'Griefing, stealing from unclaimed chests, scamming players in deals (including the auction) and circumventing anti-AFK are forbidden.',
    't.s3l3': 'Insults, hate speech, third-party advertising and spam are not allowed in chat. Chat languages are Russian and English.',
    't.s3l4': 'The administration may undo the consequences of violations (rolling back damage, confiscating unfairly obtained items).',
    't.s4h': '4. Punishments and appeals',
    't.s4p1': 'Violations may lead to a warning, mute, kick or account ban — depending on severity and repetition. Bans can be appealed in the Project community (Discord) in the appeals section.',
    't.s5h': '5. User content',
    't.s5p1': 'Builds, names and messages you create on the server remain yours. By posting content in the Project you allow the administration to showcase it on the site and in Project communities (screenshots, build features). Moderation may remove content that breaks the rules.',
    't.s6h': '6. Virtual currency and items',
    't.s6p1': 'Project coins, items and cosmetics exist only inside the game, have no monetary value and cannot be exchanged for real money. Selling accounts “with contents” for money is forbidden.',
    't.s7h': '7. Changes to the terms',
    't.s7p1': 'The administration may change the Project’s terms and mechanics. Significant changes are announced in server and community news. Continuing to play after the changes means accepting them.',
    't.s8h': '8. Contacts',
    't.s8p1': 'Questions about the terms — in the Project communities: <a href="https://discord.gg/2cuM7kpR" target="_blank" rel="noopener">Discord</a> · <a href="https://t.me/SYNAPSE_of" target="_blank" rel="noopener">Telegram</a>.',

    /* --- privacy --- */
    'pv.title': 'Privacy Policy — Loom Iven',
    'pv.desc': 'Loom Iven privacy policy: what data the server and site collect, why, how it is stored and how to have it deleted.',
    'pv.h1': 'Privacy Policy',
    'pv.meta': 'Edition of November 1, 2026 · the Loom Iven project',
    'pv.s1h': '1. What data is collected',
    'pv.s1l1': '<b>Game data:</b> nickname, account UUID, hours played, mechanic statistics (coins, achievements, contributions to funds and orders, ranks).',
    'pv.s1l2': '<b>Technical logs:</b> IP address and connection times — standard server logs, used only for moderation and incident analysis (CoreProtect, Paper logs).',
    'pv.s1l3': '<b>Discord link:</b> if you use DiscordSRV, a “Minecraft account — Discord account” pair is stored for the chat bridge and role grants.',
    'pv.s1l4': '<b>Site settings:</b> your theme and language are stored locally in your browser (localStorage) and never leave it.',
    'pv.s2h': '2. Why this data',
    'pv.s2p1': 'The data keeps gameplay mechanics working (playtime ladder, auction, passes), supports moderation and safety, and helps track the Project’s growth. Nothing is sold or passed to third parties.',
    'pv.s3h': '3. Storage',
    'pv.s3p1': 'Data is stored on the Project’s infrastructure; only the administration has access. Gameplay stats are visible to other players in public tops (hours, coins, hall of fame) — that is part of the game.',
    'pv.s4h': '4. Deletion',
    'pv.s4p1': 'You may ask the administration to delete your gameplay statistics and Discord link via <a href="https://discord.gg/2cuM7kpR" target="_blank" rel="noopener">Discord</a> or <a href="https://t.me/SYNAPSE_of" target="_blank" rel="noopener">Telegram</a>. Technical logs are kept for a limited time and removed at log rotation.',
    'pv.s5h': '5. Age',
    'pv.s5p1': 'The Project is intended for players who are allowed to play Minecraft under the Mojang EULA and local law. If you are under the required age, play with parental permission.',
    'pv.s6h': '6. Policy changes',
    'pv.s6p1': 'The current edition is always available on this page. Significant changes are announced in the Project community.',

    /* --- offer --- */
    'of.title': 'Public Offer — Loom Iven',
    'of.desc': 'Loom Iven public offer agreement: voluntary contributions, season pass, payment and refunds.',
    'of.h1': 'Public Offer Agreement',
    'of.meta': 'Edition of November 1, 2026 · the Loom Iven project',
    'of.s1h': '1. Subject of the offer',
    'of.s1p1': 'The Project administration (the “Operator”) grants any person access to the Loom Iven game server and website under this agreement. The agreement is a public offer; connecting to the server or using the site constitutes acceptance.',
    'of.s2h': '2. Free at the core',
    'of.s2p1': 'All gameplay mechanics of the Project are free. The virtual currency (coins) is earned by playing and is not bought for real money.',
    'of.s3h': '3. Voluntary contributions',
    'of.s3l1': 'The season pass has an additional paid track: 25,000 coins — the in-game price — or a ruble purchase if the shop is connected on the hosting platform.',
    'of.s3l2': 'The fee grants cosmetic advantages only, never gameplay advantages over other players: neither access to mechanics nor extra coins beyond the rewards described in the pass.',
    'of.s3l3': 'The donate track’s unique cosmetics move to the pass’s free track next season.',
    'of.s4h': '4. Payment',
    'of.s4p1': 'Payments go through the payment service of the hosting platform. The service is deemed provided once the pass status is credited to the buyer’s account.',
    'of.s5h': '5. Refunds',
    'of.s5p1': 'Contributions are voluntary. Virtual goods (pass status, cosmetics) are deemed consumed upon activation, so no refund is issued after activation. If the service was not delivered through the Operator’s fault, the status is restored or compensated next season.',
    'of.s6h': '6. Limitation of liability',
    'of.s6p1': 'The server and the site are provided “as is”. The Operator does not guarantee uninterrupted operation and is not liable for indirect losses or the loss of in-game valuables caused by technical failures or by the player’s own rule violations.',
    'of.s7h': '7. Term and changes',
    'of.s7p1': 'The offer is effective from the publication date until withdrawn by the Operator. An accepted edition continues to apply to services already rendered.',
    'of.s8h': '8. Contacts',
    'of.s8p1': 'Operator: the Loom Iven project administration — <a href="https://discord.gg/2cuM7kpR" target="_blank" rel="noopener">Discord</a> · <a href="https://t.me/SYNAPSE_of" target="_blank" rel="noopener">Telegram</a>.'
  };

  /* ---------- утилиты ---------- */
  function store(k, v) {
    try {
      if (v === undefined) return localStorage.getItem(k);
      localStorage.setItem(k, v);
    } catch (e) { return null; }
  }

  function cache(el, attr, fn) {
    if (!('__orig' in el)) el.__orig = fn(el);
    return el.__orig;
  }

  function applyLang(lang) {
    var missing = [];
    document.querySelectorAll('[data-i18n]').forEach(function (el) {
      var key = el.getAttribute('data-i18n');
      var orig = cache(el, null, function (e) { return e.textContent; });
      if (lang === 'en') {
        if (Object.prototype.hasOwnProperty.call(EN, key)) el.textContent = EN[key];
        else missing.push(key);
      } else el.textContent = orig;
    });
    document.querySelectorAll('[data-i18n-html]').forEach(function (el) {
      var key = el.getAttribute('data-i18n-html');
      var orig = cache(el, null, function (e) { return e.innerHTML; });
      if (lang === 'en') {
        if (Object.prototype.hasOwnProperty.call(EN, key)) el.innerHTML = EN[key];
        else missing.push(key);
      } else el.innerHTML = orig;
    });
    document.querySelectorAll('[data-i18n-content]').forEach(function (el) {
      var key = el.getAttribute('data-i18n-content');
      var orig = cache(el, null, function (e) { return e.getAttribute('content'); });
      el.setAttribute('content', lang === 'en' ? (EN[key] || orig) : orig);
    });
    if (missing.length) console.warn('[i18n] нет EN-ключей:', missing.join(', '));
    document.documentElement.lang = lang;
    var lbl = document.getElementById('langLabel');
    if (lbl) lbl.textContent = lang === 'ru' ? 'EN' : 'RU';
  }

  function applyTheme(t) {
    if (t === 'light') document.documentElement.setAttribute('data-theme', 'light');
    else document.documentElement.removeAttribute('data-theme');
    var use = document.querySelector('#themeBtn use');
    if (use) use.setAttribute('href', t === 'light' ? '#i-sun' : '#i-moon');
  }

  function init() {
    document.body.insertAdjacentHTML('afterbegin', SPRITE);

    var theme = store('li_theme') || 'dark';
    applyTheme(theme);
    var themeBtn = document.getElementById('themeBtn');
    if (themeBtn) themeBtn.addEventListener('click', function () {
      theme = theme === 'light' ? 'dark' : 'light';
      store('li_theme', theme);
      applyTheme(theme);
    });

    var lang = store('li_lang') || 'ru';
    applyLang(lang);
    var langBtn = document.getElementById('langBtn');
    if (langBtn) langBtn.addEventListener('click', function () {
      lang = lang === 'ru' ? 'en' : 'ru';
      store('li_lang', lang);
      applyLang(lang);
    });

    var burger = document.getElementById('burgerBtn');
    var menu = document.getElementById('mobileMenu');
    if (burger && menu) burger.addEventListener('click', function () {
      var open = menu.classList.toggle('open');
      burger.setAttribute('aria-expanded', open ? 'true' : 'false');
    });
    if (menu) menu.querySelectorAll('a').forEach(function (a) {
      a.addEventListener('click', function () { menu.classList.remove('open'); });
    });

    var here = location.pathname.split('/').pop() || 'index.html';
    document.querySelectorAll('header.site .links a, .mobile-menu a').forEach(function (a) {
      if (a.getAttribute('href') === here) a.classList.add('active');
    });
  }

  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', init);
  else init();
})();
