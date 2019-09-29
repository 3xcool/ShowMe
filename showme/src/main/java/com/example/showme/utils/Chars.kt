package com.example.showme.utils


//http://xahlee.info/comp/unicode_emoticons.html
class Chars {
  /**
   * ⭐⭕✨❌⚡❎✅☕⏰⌛⏳❓❔❗ ❕ ↻ ↺ ⏩⏪⏫⏬✋⛔ ☣☢☠ⓘ 🔍 🐞 💩 💡 🔥 👀 💀 ☠ 👻 👽 👾 🎅 💃 💁 💬 🍰 💎 💍 🎯 🏃 🏁 🏆 ☁ 🌁
   * ⛅ 🎦🌋
   * ⚛ ☠ ☢ ☣  ⛔ 🚫 ⚠ ☡ 🔧 💣 🔒 🔓🔔 ⌚ ⌛ ⏳💉🔮🎃🎊🎉🎂 💰 💱 💲 💳 💴 💵 💶 💷 💸 🚪 📨 📤 📥 📩
   * 📌 📍📎 📜 📃 📄 📅 📆 📇🔃 🏁  🚩 🎌 ⛳  ⇦ ⇨ ⇧ ⇩ ▲ ▼ ◁ ▷ △ ▽ ⇤ ⇥  ↖ ↗ ↘ ↙ 🔊
   * ✓ ✔ ⍻ 🗸 ✗ ✘ 𐄂

  //➿ ⌚⌛ ⏩⏪⏫⏬⏰⏳☔☕⚓⚡
  //⚽⛄⛅⛎⛔ ⛪⛲⛳⛵⛺⛽✅✊✋✨
  //♈♉♊♋♌♍♎♏♐♑♒♓ ♔♕♖♗♘♙♚♛♜♝♞♟♠♡♢♣♤♥♦♧♨♩♪♫♬♭♮♯♰♱♲♳♴♵♶♷♸♹♺♻♼♽♾♿⚀⚁⚂⚃⚄⚅ ⚢⚣⚤⚥⚦⚧⚨
   *
   * " ↻ ↺ ↯ ➲➢☣☢☠ © ® ™ π∞±∏∑∫ 〠〼Ѻ⊿∧∨∠∇∆∽∄ •◦‣✓●■◆○□◇★☆♠♣♦♥♤♧♢♡ ✝✚✡☥☭☪☮☺☹☯\n" + "↶↷◀▶▲▼ ◁▷△▽ ☐☑☒☓☖☗☡\n"
  + "← ↑ → ↓ ↔ ↕ ↖ ↗ ↘ ↙ ⇄ ⇅ ⇆ ⇇ ⇈ ⇉ ⇊ ⇋ ⇌ ⇍ ⇎ ⇏ ⇐ ⇑ ⇒ ⇓ ⇔ ⇕ ⇖ ⇗ ⇘ ⇙ ⇚ ⇛ ⇜\n"
  + "➘➙➚➛➜➝➞➟➠➡➢➣➤➥➦➧➨➩➪➫➬➭➮➯➰➱➲➳➴➵➶➷➸➹➺➻➼➽➾\n"
  + "ﻼ\uFEFD\uFEFE\uFEFF\uFF00！＂＃＄％＆＇（）＊＋，－．／０１２３４５６７８９：；＜＝＞？＠ＡＢＣＤＥＦＧＨＩＪＫＬＭＮＯＰＱＲＳＴＵＶＷＸＹＺ［＼］＾＿｀ａｂｃｄｅｆｇｈｉｊｋｌｍｎｏｐｑｒｓｔｕｖｗｘｙｚ｛｜｝～\n"
  + "\n" + "➀➁➂➃➄➅➆➇➈➉ ⁰¹²³\n" + "➊➋➌➍➎➏➐➑➒➓⓫⓬⓭⓮⓯⓰⓱⓲⓳⓴ \n" + "❶❷❸❹❺❻❼❽❾❿\n" + "\n" + "│┄ ┅┆┇ ∞∟∠∡∢∣∤∥∦∧∨∩∪∫\n"
  + "←↑→↓↔↕↖↗↘↙⇒⇓⇔⇕⇖⇗⇘⇙⇚⇛⇜⇝⇞⇟⇠⇡⇢⇣⇤⇥⇦⇧⇨⇩⇪∀∁∂∃∄∅∆∇∈∉∊∋∌∍∎∏∐∑−∓∔∕∖∗∘∙√∛∜∝∞∟∠∡∢∣∤∥∦∧∨∩∪∫∬∭∮∯∰∱∲∳\n"
  + "≠≡ ≢ ≣≤≥≦≧≨≩≪≫≬≭≮≯≰≱≲≳≴≵≶≷≸≹≺≻≼≽≾≿⊀⊁⊂⊃⊄⊅⊆⊇⊈⊉⊊⊋⊌⊍⊎⊏⊐⊑⊒⊓⊔⊕⊖⊗⊘⊙⊚⊛⊜⊝⊞⊟⊠⊡⊢⊣⊤⊥⊦⊧⊨⊩⊪⊫⊬⊭⊮⊯⊰⊱⊲⊳⊴⊵⊶⊷⊸⊹⊺⊻⊼⊽⊾⊿⋀⋁⋂⋃⋄\n"
  + "⌂⌒⌘⌠⌡␣①②③④⑤⑥⑦⑧⑨⑩⑪⑫⑬⑭⑮⑯⑰⑱⑲⑳\n"
  + "// ⓪⓵⓶⓷⓸⓹⓺⓻⓼⓽⓾─━│┃┄┅┆┇┈┉┊┋┌┍┎┏┐┑┒┓└┕┖┗┘┙┚┛├┝┞┟┠┡┢┣┤┥┦┧┨┩┪┫┬┭┮┯┰┱┲┳┴┵┶┷┸┹┺┻┼┽┾┿╀╁╂╃╄╅╆╇╈╉╊╋\n"
  + "═║╒╓╔╕╖╗╘╙╚╛╜╝╞╟╠╡╢╣╤╥╦╧╨╩╪╫╬▀▄█▌▍▎▏▐░▒▓▔■□▢▣▤▥▦▧▨▩▪▫▬▭▮▯▰▱▲△▴▵▶▷▸▹►▻▼▽▾▿◀◁◂◃◄◅◆◇◈◉◊○◌◍◎●◐◑◒◓◔◕◖◗◘◙◦\n"
  + "◬◭◮◯☀☁☂☃☄★☆☇☈☉ꖴ☊☋☌☍☎☏☐☑☒☓☔☕☖☗☘☙☚☛☜☝☞☟☠☡☢☣☤☥☦☧☨☩☪☫☬☭☮☯☸☹☺☻ ☼☽☾♁♂\n" + "♔♕♖♗♘♙♚♛♜♝♞♟♠♡♢♣♤♥♦♧♨♩♪♫♬♭♮♯\n"
  + "✁✂✃✄✆✇✈✉✌✍✎✏✐✑✒✓✔✕✖✗✘✙✚✛✜✝✞✟✠✡✢✣✤✥✦✧✨✩✪✫✬✭✮✯✰✱✲✳✴✵✶✷✸✹✺✻✼✽✾✿❀❁❂❃❄❅❆❇❈❉❊❋\n" + "❍❏❐❑❒❖❗❘❙❚❛❜❤❥\n"
  + "➔➘➙➚➛➜➝➞➟➠➡➢➣➤➥➦➧➨➩➪➫➬➭➮➯➰➱➲➳➴➵➶➷➸➹➺➻➼➽➾"
   */

}