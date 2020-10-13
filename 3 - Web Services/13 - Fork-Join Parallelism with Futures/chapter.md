```scala
def fetchAllLinksParallel(startTitle: String, depth: Int): Set[String] = {
  var seen = Set(startTitle)
  var current = Set(startTitle)
  for (i <- Range(0, depth)) {
    val futures = for (title <- current) yield Future{ fetchLinks(title) }
    val nextTitleLists = futures.map(Await.result(_, Inf))
    current = nextTitleLists.flatten.filter(!seen.contains(_))
    seen = seen ++ current
  }
  seen
}
```

`%Snippet 13.1: a simple parallel web-crawler implemented using Scala Futures`

The Scala programming language comes with a Futures API. Futures make parallel
and asynchronous programming much easier to handle than working with traditional
techniques of threads, locks, and callbacks.

This chapter dives into Scala's Futures: how to use them, how they work, and how
you can use them to parallelize data processing workflows. It culminates in
using Futures together with the techniques we learned in `%Chapter 12: Working
with HTTP APIs` to write a high-performance concurrent web crawler in a
straightforward and intuitive way.

-------------------------------------------------------------------------------


# Parallel Computation using Futures

Facilisis vol utpat est velit egestas dui id ornare arcu. Vestibulum rhoncus est
pellentesque elit ullamcorper. Purus in mollis nunc sed id semper risus. Quisque
egestas diam in arcu. Pellentesque diam volutpat commodo sed. Tellus
pellentesque eu tincidunt tortor aliquam nulla facilisi cras. Vestibulum lorem
sed risus ultricies tristique nulla aliquet enim. Rutrum tellus pellentesque eu
tincidunt tortor aliquam nulla facilisi cras. Vitae nunc sed velit dignissim
sodales ut eu. Suscipit adipiscing bibendum est ultricies integer quis auctor
elit sed.

Sagittis vita e et leo duis. In aliquam sem fringilla ut morbi tincidunt augue.
In pellentesque massa placerat duis ultricies lacus sed. Commodo quis imperdiet
massa tincidunt nunc pulvinar sapien et. In nulla posuere sollicitudin aliquam
ultrices. Ullamcorper sit amet risus nullam eget felis eget nunc lobortis. Sed
cras ornare arcu dui vivamus arcu felis. Augue mauris augue neque gravida. Eu
nisl nunc mi ipsum faucibus vitae aliquet. Mauris cursus mattis molestie a
iaculis at erat pellentesque. Aliquam nulla facilisi cras fermentum odio eu. Eu
non diam phasellus vestibulum lorem sed risus. Sodales neque sodales ut etiam.
Blandit libero volutpat sed cras ornare. Tortor at risus viverra adipiscing.
Quis eleifend quam adipiscing vitae proin sagittis. Tincidunt lobortis feugiat
vivamus at. Nibh cras pulvinar mattis nunc sed blandit libero volutpat sed.
Congue eu consequat ac felis donec et odio. Platea dictumst quisque sagittis
purus.

Vitae aliquet  nec ullamcorper sit amet risus. Donec massa sapien faucibus et
molestie. Eu mi bibendum neque egestas congue. Mi in nulla posuere sollicitudin
aliquam ultrices sagittis orci a. Sem integer vitae justo eget magna fermentum
iaculis eu non. Tellus integer feugiat scelerisque varius morbi. Dolor morbi non
arcu risus quis varius quam quisque id. Vel orci porta non pulvinar neque
laoreet. Egestas fringilla phasellus faucibus scelerisque eleifend donec pretium
vulputate sapien. Duis ultricies lacus sed turpis tincidunt id aliquet risus
feugiat. Elementum nibh tellus molestie nunc non blandit massa enim. Placerat
vestibulum lectus mauris ultrices eros. Quis auctor elit sed vulputate mi sit
amet mauris. Commodo sed egestas egestas fringilla phasellus. Cursus sit amet
dictum sit amet justo donec enim diam. Ante metus dictum at tempor commodo
ullamcorper a lacus vestibulum.

Turpis egest as sed tempus urna et pharetra. Diam ut venenatis tellus in metus
vulputate eu scelerisque. Nisl pretium fusce id velit ut. Eu sem integer vitae
justo. Quis auctor elit sed vulputate mi sit amet mauris commodo. Non odio
euismod lacinia at. Ut eu sem integer vitae justo eget magna fermentum. Molestie
a iaculis at erat pellentesque adipiscing. At volutpat diam ut venenatis. At
varius vel pharetra vel. Nibh venenatis cras sed felis eget velit. Eget nulla
facilisi etiam dignissim. Eget magna fermentum iaculis eu non diam phasellus
vestibulum. Ante metus dictum at tempor commodo ullamcorper a lacus vestibulum.
Vestibulum sed arcu non odio. Et sollicitudin ac orci phasellus egestas tellus.
Cursus euismod quis viverra nibh. Fermentum leo vel orci porta non.

Quis vel ero s donec ac. Id faucibus nisl tincidunt eget nullam. Porttitor lacus
luctus accumsan tortor. Laoreet id donec ultrices tincidunt. Sit amet est
placerat in egestas erat imperdiet. Amet volutpat consequat mauris nunc congue
nisi. Nulla facilisi etiam dignissim diam quis enim lobortis scelerisque. Ipsum
dolor sit amet consectetur adipiscing. Sagittis aliquam malesuada bibendum arcu
vitae elementum curabitur. Quisque egestas diam in arcu cursus euismod quis
viverra nibh. Vel facilisis volutpat est velit egestas. Laoreet non curabitur
gravida arcu ac tortor dignissim convallis. Non odio euismod lacinia at quis
risus.

Dolor sit am et consectetur adipiscing elit pellentesque. Risus viverra
adipiscing at in tellus integer. Sollicitudin aliquam ultrices sagittis orci a
scelerisque purus semper. Non sodales neque sodales ut. Facilisis sed odio morbi
quis commodo odio aenean sed. Tortor posuere ac ut consequat semper viverra nam
libero. Semper feugiat nibh sed pulvinar. Ac orci phasellus egestas tellus
rutrum tellus. Vitae nunc sed velit dignissim sodales ut eu sem. Diam vulputate
ut pharetra sit amet. Tellus elementum sagittis vitae et leo duis ut diam quam.

Lorem ipsum  dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor
incididunt ut labore et dolore magna aliqua. Nulla pharetra diam sit amet nisl
suscipit adipiscing bibendum. Elit at imperdiet dui accumsan sit amet nulla
facilisi morbi. Condimentum id venenatis a condimentum vitae sapien
pellentesque. Malesuada bibendum arcu vitae elementum curabitur vitae nunc sed
velit. Ultricies integer quis auctor elit sed vulputate. Egestas dui id ornare
arcu odio ut sem nulla pharetra. Amet est placerat in egestas erat. Id cursus
metus aliquam eleifend mi in nulla posuere sollicitudin. Nulla aliquet porttitor
lacus luctus accumsan tortor. Blandit massa enim nec dui nunc mattis enim.
Pulvinar proin gravida hendrerit lectus. Sodales neque sodales ut etiam sit
amet. Amet consectetur adipiscing elit duis tristique. Ut tortor pretium viverra
suspendisse. Cursus euismod quis viverra nibh cras pulvinar. Odio eu feugiat
pretium nibh ipsum. Morbi blandit cursus risus at ultrices mi tempus imperdiet
nulla. Aliquam eleifend mi in nulla posuere sollicitudin aliquam ultrices
sagittis.

Fermentum dui  faucibus in ornare quam viverra orci. Iaculis eu non diam
phasellus. Aliquam malesuada bibendum arcu vitae elementum curabitur vitae nunc
sed. Elementum nibh tellus molestie nunc non blandit massa enim nec. Nulla
facilisi cras fermentum odio eu feugiat. Porttitor massa id neque aliquam
vestibulum morbi. Elit sed vulputate mi sit. Sed felis eget velit aliquet
sagittis. Bibendum arcu vitae elementum curabitur vitae nunc sed. Consequat id
porta nibh venenatis. Pretium quam vulputate dignissim suspendisse. In est ante
in nibh. Ut sem nulla pharetra diam sit amet nisl suscipit. Dui accumsan sit
amet nulla facilisi. In nulla posuere sollicitudin aliquam ultrices sagittis
orci a scelerisque. Ut consequat semper viverra nam libero justo laoreet sit.

# N-Ways Parallelism

Morbi tincidunt  ornare massa eget egestas purus viverra accumsan in. Dignissim
cras tincidunt lobortis feugiat vivamus at augue. In dictum non consectetur a
erat nam. Eu scelerisque felis imperdiet proin fermentum leo vel. Nulla aliquet
enim tortor at auctor urna nunc. Egestas pretium aenean pharetra magna ac
placerat vestibulum lectus. Orci dapibus ultrices in iaculis nunc. Magna etiam
tempor orci eu lobortis elementum nibh tellus. Velit sed ullamcorper morbi
tincidunt ornare massa eget. Arcu non odio euismod lacinia at quis risus sed
vulputate. Arcu bibendum at varius vel pharetra vel. Non quam lacus suspendisse
faucibus interdum. Enim nunc faucibus a pellentesque sit amet porttitor.

Platea dictumst  vestibulum rhoncus est pellentesque elit ullamcorper. Cras
semper auctor neque vitae tempus quam pellentesque nec nam. Habitasse platea
dictumst vestibulum rhoncus est pellentesque. Volutpat blandit aliquam etiam
erat velit. Sit amet aliquam id diam maecenas. A diam maecenas sed enim ut sem
viverra aliquet eget. Tincidunt dui ut ornare lectus sit amet est placerat. Erat
velit scelerisque in dictum. Amet nisl suscipit adipiscing bibendum. Ornare quam
viverra orci sagittis eu. Dignissim convallis aenean et tortor at. Sed tempus
urna et pharetra pharetra massa massa ultricies mi. Venenatis urna cursus eget
nunc scelerisque viverra mauris in. Enim lobortis scelerisque fermentum dui
faucibus in. Nulla malesuada pellentesque elit eget.

# Parallel Web Crawling

Mauris rhoncus  aenean vel elit scelerisque mauris pellentesque. Urna et pharetra
pharetra massa massa ultricies mi quis hendrerit. Non tellus orci ac auctor
augue mauris augue neque gravida. Nulla facilisi morbi tempus iaculis. Dignissim
convallis aenean et tortor at risus. Egestas sed tempus urna et pharetra
pharetra. Netus et malesuada fames ac turpis egestas maecenas pharetra. In
aliquam sem fringilla ut morbi tincidunt augue interdum. Neque egestas congue
quisque egestas diam in arcu cursus euismod. Diam in arcu cursus euismod quis
viverra nibh cras pulvinar.

Commodo nulla  facilisi nullam vehicula ipsum a. Turpis egestas sed tempus urna
et pharetra pharetra massa massa. Diam vulputate ut pharetra sit amet. Fusce id
velit ut tortor pretium. Elit ullamcorper dignissim cras tincidunt lobortis
feugiat vivamus at. Sed turpis tincidunt id aliquet risus feugiat in ante metus.
Sollicitudin ac orci phasellus egestas tellus rutrum. Nisl vel pretium lectus
quam id leo. Dignissim diam quis enim lobortis scelerisque fermentum dui. Turpis
egestas sed tempus urna et.

Risus at ultrices mi tempus imperdiet nulla malesuada. Tellus integer feugiat scelerisque varius morbi enim nunc faucibus. Convallis convallis tellus id interdum velit laoreet id. Tortor at risus viverra adipiscing at. Risus commodo viverra maecenas accumsan lacus vel. Lectus nulla at volutpat diam ut venenatis tellus in metus. Faucibus scelerisque eleifend donec pretium vulputate. Nec feugiat nisl pretium fusce id. Mattis pellentesque id nibh tortor id. Quam pellentesque nec nam aliquam sem et. Et odio pellentesque diam volutpat commodo sed. Ac odio tempor orci dapibus. Suscipit tellus mauris a diam maecenas sed enim ut sem. Leo vel fringilla est ullamcorper eget. Fringilla est ullamcorper eget nulla facilisi etiam dignissim. Augue interdum velit euismod in pellentesque. Eget arcu dictum varius duis at consectetur.

Aliquam vestibulum  morbi blandit cursus risus at ultrices mi. Elementum
curabitur vitae nunc sed velit dignissim sodales ut eu. Non curabitur gravida
arcu ac. Arcu felis bibendum ut tristique et egestas quis ipsum suspendisse.
Morbi tempus iaculis urna id volutpat. Risus ultricies tristique nulla aliquet
enim tortor at auctor urna. Elementum sagittis vitae et leo duis ut.
Pellentesque habitant morbi tristique senectus et netus et malesuada fames.
Pellentesque id nibh tortor id. Viverra suspendisse potenti nullam ac tortor
vitae purus faucibus. Porttitor rhoncus dolor purus non enim. Praesent semper
feugiat nibh sed pulvinar proin gravida hendrerit. Elit eget gravida cum sociis
natoque. Fusce ut placerat orci nulla pellentesque dignissim. Non nisi est sit
amet facilisis.

# Asynchronous Futures

Lacus luctus  accumsan tortor posuere ac ut consequat semper viverra. Arcu cursus
euismod quis viverra nibh cras pulvinar mattis. Arcu risus quis varius quam. Dis
parturient montes nascetur ridiculus. Et tortor at risus viverra. Volutpat ac
tincidunt vitae semper quis lectus nulla. Ac turpis egestas maecenas pharetra
convallis posuere morbi leo urna. Non odio euismod lacinia at. Nibh ipsum
consequat nisl vel. Interdum varius sit amet mattis vulputate. In cursus turpis
massa tincidunt dui ut ornare. Massa sed elementum tempus egestas. Massa
ultricies mi quis hendrerit dolor magna. Ac tortor dignissim convallis aenean
et. Morbi leo urna molestie at elementum. Vitae justo eget magna fermentum
iaculis eu non diam phasellus. Cursus risus at ultrices mi tempus.

Velit egesta s dui id ornare arcu odio ut sem nulla. Curabitur vitae nunc sed
velit dignissim. Quam pellentesque nec nam aliquam sem et tortor. Volutpat
maecenas volutpat blandit aliquam etiam erat velit scelerisque in. Consectetur
adipiscing elit pellentesque habitant morbi tristique senectus. Tellus mauris a
diam maecenas sed enim ut sem viverra. Nisl vel pretium lectus quam id leo in.
Malesuada bibendum arcu vitae elementum. Elit sed vulputate mi sit amet. Commodo
ullamcorper a lacus vestibulum sed arcu. Tortor at risus viverra adipiscing at
in tellus integer feugiat. Lacus luctus accumsan tortor posuere ac. Amet justo
donec enim diam vulputate ut. Convallis tellus id interdum velit laoreet id
donec ultrices. Turpis massa sed elementum tempus egestas sed. Nascetur
ridiculus mus mauris vitae ultricies leo. Maecenas pharetra convallis posuere
morbi. Magna eget est lorem ipsum dolor. Enim nulla aliquet porttitor lacus
luctus.

Sed risus ul tricies tristique nulla aliquet enim tortor at auctor. Pretium
viverra suspendisse potenti nullam. Suspendisse interdum consectetur libero id.
Odio morbi quis commodo odio aenean sed adipiscing diam. Eget aliquet nibh
praesent tristique. Aliquet risus feugiat in ante metus dictum. At elementum eu
facilisis sed odio morbi. Accumsan tortor posuere ac ut consequat semper
viverra. Sed tempus urna et pharetra pharetra. Facilisis sed odio morbi quis
commodo odio aenean. Phasellus vestibulum lorem sed risus ultricies tristique
nulla. Auctor eu augue ut lectus arcu bibendum. Sollicitudin nibh sit amet
commodo nulla facilisi nullam vehicula ipsum. Lorem mollis aliquam ut porttitor
leo. Egestas erat imperdiet sed euismod nisi porta lorem mollis. Venenatis a
condimentum vitae sapien pellentesque habitant. Nibh mauris cursus mattis
molestie a iaculis at erat pellentesque. Senectus et netus et malesuada fames.
Etiam dignissim diam quis enim lobortis scelerisque fermentum.

# Asynchronous Web Crawling

At tempor co mmodo ullamcorper a lacus vestibulum sed arcu non. Tristique
senectus et netus et. Ac turpis egestas maecenas pharetra convallis posuere
morbi leo. Feugiat sed lectus vestibulum mattis ullamcorper velit. Vel eros
donec ac odio tempor. Elementum nisi quis eleifend quam. Lacinia at quis risus
sed vulputate. Dolor sit amet consectetur adipiscing elit duis tristique. Vel
facilisis volutpat est velit egestas dui id. At urna condimentum mattis
pellentesque id nibh tortor.

Sociis natoq ue penatibus et magnis dis parturient montes nascetur ridiculus.
Dolor sit amet consectetur adipiscing elit duis tristique. Sed risus ultricies
tristique nulla. Dolor purus non enim praesent elementum. Tincidunt ornare massa
eget egestas. Et odio pellentesque diam volutpat commodo sed egestas. Dui
faucibus in ornare quam viverra orci sagittis eu volutpat. Mattis pellentesque
id nibh tortor id aliquet. Amet risus nullam eget felis eget nunc lobortis.
Risus ultricies tristique nulla aliquet enim tortor at auctor. Sit amet massa
vitae tortor condimentum. Cras ornare arcu dui vivamus arcu felis.

Facilisis vol utpat est velit egestas dui id ornare arcu. Vestibulum rhoncus est
pellentesque elit ullamcorper. Purus in mollis nunc sed id semper risus. Quisque
egestas diam in arcu. Pellentesque diam volutpat commodo sed. Tellus
pellentesque eu tincidunt tortor aliquam nulla facilisi cras. Vestibulum lorem
sed risus ultricies tristique nulla aliquet enim. Rutrum tellus pellentesque eu
tincidunt tortor aliquam nulla facilisi cras. Vitae nunc sed velit dignissim
sodales ut eu. Suscipit adipiscing bibendum est ultricies integer quis auctor
elit sed.

Sagittis vita e et leo duis. In aliquam sem fringilla ut morbi tincidunt augue.
In pellentesque massa placerat duis ultricies lacus sed. Commodo quis imperdiet
massa tincidunt nunc pulvinar sapien et. In nulla posuere sollicitudin aliquam
ultrices. Ullamcorper sit amet risus nullam eget felis eget nunc lobortis. Sed
cras ornare arcu dui vivamus arcu felis. Augue mauris augue neque gravida. Eu
nisl nunc mi ipsum faucibus vitae aliquet. Mauris cursus mattis molestie a
iaculis at erat pellentesque. Aliquam nulla facilisi cras fermentum odio eu. Eu
non diam phasellus vestibulum lorem sed risus. Sodales neque sodales ut etiam.
Blandit libero volutpat sed cras ornare. Tortor at risus viverra adipiscing.
Quis eleifend quam adipiscing vitae proin sagittis. Tincidunt lobortis feugiat
vivamus at. Nibh cras pulvinar mattis nunc sed blandit libero volutpat sed.
Congue eu consequat ac felis donec et odio. Platea dictumst quisque sagittis
purus.

Vitae aliquet  nec ullamcorper sit amet risus. Donec massa sapien faucibus et
molestie. Eu mi bibendum neque egestas congue. Mi in nulla posuere sollicitudin
aliquam ultrices sagittis orci a. Sem integer vitae justo eget magna fermentum
iaculis eu non. Tellus integer feugiat scelerisque varius morbi. Dolor morbi non
arcu risus quis varius quam quisque id. Vel orci porta non pulvinar neque
laoreet. Egestas fringilla phasellus faucibus scelerisque eleifend donec pretium
vulputate sapien. Duis ultricies lacus sed turpis tincidunt id aliquet risus
feugiat. Elementum nibh tellus molestie nunc non blandit massa enim. Placerat
vestibulum lectus mauris ultrices eros. Quis auctor elit sed vulputate mi sit
amet mauris. Commodo sed egestas egestas fringilla phasellus. Cursus sit amet
dictum sit amet justo donec enim diam. Ante metus dictum at tempor commodo
ullamcorper a lacus vestibulum.

Turpis egest as sed tempus urna et pharetra. Diam ut venenatis tellus in metus
vulputate eu scelerisque. Nisl pretium fusce id velit ut. Eu sem integer vitae
justo. Quis auctor elit sed vulputate mi sit amet mauris commodo. Non odio
euismod lacinia at. Ut eu sem integer vitae justo eget magna fermentum. Molestie
a iaculis at erat pellentesque adipiscing. At volutpat diam ut venenatis. At
varius vel pharetra vel. Nibh venenatis cras sed felis eget velit. Eget nulla
facilisi etiam dignissim. Eget magna fermentum iaculis eu non diam phasellus
vestibulum. Ante metus dictum at tempor commodo ullamcorper a lacus vestibulum.
Vestibulum sed arcu non odio. Et sollicitudin ac orci phasellus egestas tellus.
Cursus euismod quis viverra nibh. Fermentum leo vel orci porta non.

Quis vel ero s donec ac. Id faucibus nisl tincidunt eget nullam. Porttitor lacus
luctus accumsan tortor. Laoreet id donec ultrices tincidunt. Sit amet est
placerat in egestas erat imperdiet. Amet volutpat consequat mauris nunc congue
nisi. Nulla facilisi etiam dignissim diam quis enim lobortis scelerisque. Ipsum
dolor sit amet consectetur adipiscing. Sagittis aliquam malesuada bibendum arcu
vitae elementum curabitur. Quisque egestas diam in arcu cursus euismod quis
viverra nibh. Vel facilisis volutpat est velit egestas. Laoreet non curabitur
gravida arcu ac tortor dignissim convallis. Non odio euismod lacinia at quis
risus.

# Conclusion

Dolor sit am et consectetur adipiscing elit pellentesque. Risus viverra
adipiscing at in tellus integer. Sollicitudin aliquam ultrices sagittis orci a
scelerisque purus semper. Non sodales neque sodales ut. Facilisis sed odio morbi
quis commodo odio aenean sed. Tortor posuere ac ut consequat semper viverra nam
libero. Semper feugiat nibh sed pulvinar. Ac orci phasellus egestas tellus
rutrum tellus. Vitae nunc sed velit dignissim sodales ut eu sem. Diam vulputate
ut pharetra sit amet. Tellus elementum sagittis vitae et leo duis ut diam quam.

Pretium quam vulputate dignissim suspendisse in est ante in. Adipiscing elit
pellentesque habitant morbi tristique senectus et. Risus in hendrerit gravida
rutrum. Lectus proin nibh nisl condimentum id venenatis a. Dui faucibus in
ornare quam viverra orci. Turpis egestas pretium aenean pharetra magna ac. Dolor
sit amet consectetur adipiscing. Nulla posuere sollicitudin aliquam ultrices
sagittis. Amet justo donec enim diam vulputate ut pharetra sit. Aliquet bibendum
enim facilisis gravida neque. Varius quam quisque id diam. Donec enim diam
vulputate ut pharetra. Vivamus at augue eget arcu dictum varius duis. Leo
integer malesuada nunc vel risus commodo viverra. Vitae proin sagittis nisl
rhoncus mattis rhoncus. Pellentesque eu tincidunt tortor aliquam nulla facilisi.
Amet justo donec enim diam. Ac orci phasellus egestas tellus rutrum tellus
pellentesque eu tincidunt.