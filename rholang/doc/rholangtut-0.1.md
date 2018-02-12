# A Rholang tutorial
Rholang adalah bahasa pemrograman yang dirancang untuk digunakan pada sistem terdistribusi. Seperti semua hal yang baru lahir, berkembang dan berubah dengan cepat; dokumen ini menjelaskan sintaks yang akan digunakan dalam 0.1 rilis SDK.

Rholang adalah "proses-berorientasi": semua perhitungan dilakukan dengan cara message passing. Pesan yang disampaikan pada "saluran", yang lebih suka pesan antrian tapi berperilaku seperti set daripada antrian. Rholang adalah benar-benar asynchronous, dalam arti bahwa sementara anda bisa membaca pesan dari saluran dan kemudian melakukan sesuatu dengan itu, anda tidak dapat mengirim pesan dan kemudian melakukan sesuatu setelah itu telah diterima---setidaknya, tidak secara eksplisit tanpa menunggu pengakuan pesan dari penerima.

## Kontrak dan pengiriman data

    1 new helloWorld in {
    2   contract helloWorld(name) = {
    3     "Hello, ".display(name, "!\n")
    4   } |
    5   helloWorld("Joe")
    6 }

1) Rholang program adalah suatu proses tunggal. Proses ini dimulai dengan membuat saluran baru bernama `helloWorld`. Untuk membuat yang baru, private channel, kami menggunakan `baru ... di` konstruksi. Tidak ada proses lain dapat mengirim atau menerima pesan melalui saluran ini kecuali kita secara eksplisit mengirim saluran ini untuk lain 1) Rholang program adalah suatu proses tunggal. Proses ini dimulai dengan membuat saluran baru bernama `helloWorld`. Untuk membuat yang baru, private channel, kami menggunakan `baru ... di` konstruksi. Tidak ada proses lain dapat mengirim atau menerima pesan melalui saluran ini kecuali kita secara eksplisit mengirim saluran ini ke proses lainnya.proses.

2) `kontrak` produksi menciptakan suatu proses yang menumbuhkan copy dari tubuh setiap kali menerima pesan.

3) `display` metode string menulis standar. Dibutuhkan daftar string untuk mencetak berikutnya. Oleh karena itu, untuk ini untuk bekerja, pesan `nama` harus berupa string.

5) Kami mengirim string `"Joe"` channel `helloWorld`.

## Menerima data

     1 new helloAgain in {
     2   contract helloAgain(_) = {
     3     new chan in {
     4       chan("Hello again, world!") |
     5       for (text <- chan) {
     6         text.display("\n")
     7       }
     8     }
     9   } |
    10   helloAgain(Nil)
    11 }

2) Kontrak mengambil setidaknya satu parameter, tapi kita bisa membuangnya dengan mengikat untuk variabel kita tidak pernah menggunakan

3) We create a new channel `chan`.

4) We send the string process `"Hello again, world!"` over the new channel.

5) Kita mendengarkan pada saluran baru untuk satu pesan. `Untuk` operasi blok sampai ada pesan yang tersedia pada saluran `chan`. `Untuk` operasi seperti kontrak, kecuali bahwa itu hanya membaca satu pesan dan kemudian menjadi tubuh bukan king copy dari tubuh untuk setiap pesan.

## Mutable state

     1 new MakeCell in {
     2   // Makes a single cell in which you can store values
     3   contract MakeCell(init, get, set) = {
     4     new valueStore in {
     5       valueStore(init) |
     6       contract get(ack) = {
     7         for(value <- valueStore) {
     8           valueStore(value) | ack(value)
     9         }
    10       } |
    11       contract set(pair) = {
    12         for(_ <- valueStore) {
    13           match pair with [newValue, ack] => {
    14             valueStore(newValue) | ack(Nil)
    15           }
    16         }
    17       }
    18     }
    19   } |
    20   // Cell usage.
    21   new myGet, mySet in {
    22     MakeCell(123, myGet, mySet) |
    23     new ack in {
    24       myGet(ack) |
    25       for (result <- ack) {
    26         result.display("\n") |
    27         mySet([456, ack]) |
    28         for (_ <- ack) {
    29           myGet(ack) |
    30           for (result <- ack) {
    31             result.display("\n")
    32           }
    33         }
    34       }
    35     }
    36   }
    37 }

1) Kami membuat saluran baru MakeCell dan kemudian menggunakannya pada line 3 sebagai nama dari internal kontrak. Tidak ada proses lain dari kode ini dalam leksikal lingkup dapat memanggil itu.

3) The `MakeCell` contract takes three arguments.  The first argument is the initial value to be stored in the cell.  The second and third arguments are channels over which the cell will receive requests to get and set the value.

4) To store the value, we create a new channel.  This channel will have at most one message on it containing the current value of the cell.  

5) Before this line, there are no messages on the `valueStore` channel.  After we send the initial value, it is the only value on that channel.

6) We set up a contract to listen on the `get` channel.  Each time a message is sent on `get`, the body of the contract will be executed

7) We block until we get one message from the `valueStore` channel.  Because there is at most one message ever waiting on `valueStore`, reading the message behaves much like acquiring a lock.

8) We send the current value on `valueStore` again, allowing other messages to be processed, and we send the current value back to the client on the `ack` channel.

11) In parallel with the `get` contract, we run a contract listening on `set`.

12) We block until there's a message on `valueStore`, then read it.  We throw away the message that we read.

13) The `match` operation does destructuring bind, splitting up the tuple `pair` into its components and assigning names to them.

14) We send the new value to store on `valueStore` and signal that the operation is complete.

21-36) The usage code demonstrates creating a cell, assigning the initial value 123, getting and printing that value, setting the value to 456, then getting and printing that value.  

Note the deep layers of callback. Rholang was designed to make parallel computations natural to express; as a consequence, data dependencies implicit in sequencing in other languages must be made explicit.

## Iteration and matching

In the code below, `iterate` first sends a channel `next` over `iterator`, and then for each message received on `next` sends a pair containing the next item in the list and whether the iteration is done.

     1 new iterator, iterate in {
     2     contract iterate(list, iterator) = {
     3         new next, right in {
     4             iterator(next) |
     5             for (_ <- next) {
     6                 contract right(pair) = {
     7                     match pair with [i, limit] => {
     8                         iterator([list.nth(i), i < limit]) |
     9                         for (_ <- next) {
    10                             match i + 1 < limit with true => {
    11                                 right([i + 1, limit]) 
    12                             }
    13                         }
    14                     }
    15                 } |
    16                 right([0, list.size()])
    17             }
    18         }
    19     } |
    20     // Invoke the iterator contract on channel
    21     iterate([4,5,6], iterator) |
    22     
    23     // Interacts with the iterator
    24     for (next <- iterator) {
    25         next(Nil) |
    26         new left in {
    27             contract left(_) = {
    28                 for (pair <- iterator) {
    29                     match pair with [v, keepGoing] => {
    30                         v.display("\n") |
    31                         match keepGoing with true => { 
    32                             next(Nil) |
    33                             left(Nil) 
    34                         }
    35                     }
    36                 }
    37             } |
    38             left(Nil)
    39         }
    40     }
    41 }

7) The `match .. with` construction allows destructuring bind.

8) The `nth` method on tuples allows extracting individual elements.

16) Tuples have a `size` method.

## Maps

     1 new MakeCoatCheck in {
     2     contract MakeCoatCheck(ret) = {
     3         new port, mapStore in {
     4             mapStore(Map()) |
     5             ret(port) |
     6             contract port (method, ack, arg1, arg2) = {
     7                 match method with
     8                 "new" => {
     9                     for (map <- mapStore) {
    10                         new ticket in {
    11                             map.insert(ticket, arg1) |
    12                             mapStore(map) |
    13                             ack(ticket)
    14                         }            
    15                     }
    16                 }
    17                 "get" => {
    18                     for (map <- mapStore) {
    19                         mapStore(map) |
    20                         ack(map.get(arg1))
    21                     }
    22                 }
    23                 "set" => {
    24                     for (map <- mapStore) {
    25                         map.insert(arg1, arg2) |
    26                         mapStore(map) |
    27                         ack(Nil)
    28                     }
    29                 }
    30             }
    31         }
    32     } |
    33 
    34     // Usage
    35     new ret in {
    36         MakeCoatCheck(ret) |
    37         for (cc <- ret) {
    38             // Creates new cell with initial value 0
    39             cc("new", ret, 0, Nil) |
    40             for (ticket <- ret) {
    41                 // Sets the cell to 1
    42                 cc("set", ret, ticket, 1) |
    43                 for (ack <- ret) {
    44                     // Reads the value
    45                     cc("get", ret, ticket, Nil) |
    46                     for (storedValue <- ret) {
    47                         // Prints 1
    48                         storedValue.display("\n")
    49                     }
    50                 }
    51             }
    52         }
    53     }
    54 }

2) salah Satu pola desain, digunakan dalam MakeCell kontrak di atas, adalah untuk menerima dari pemanggil saluran untuk masing-masing bagian yang berbeda dari fungsi yang proses menyediakan. Berorientasi objek programmer mungkin mengatakan bahwa MakeCell memerlukan pemanggil untuk menyediakan saluran untuk masing-masing metode. Pertandingan yang berusaha dalam urutan mereka muncul dalam kode; jika tidak ada pertandingan yang terjadi, `setara` block mengevaluasi untuk `Nihil` proses. MakeCoatCheck menggunakan pendekatan yang lebih berorientasi obyek, seperti yang akan kita lihat.

3-4) Each coat check has its own mutable reentrant map in which to store items.  We store the newly constructed map on mapStore.  It has the following API:

    insert(key, value)
    insertMany(key1, val1, key2, val2, ..., keyn, valn)
    getOrElse(key, default)
    get(key)

6) We expect four arguments every time; we could also have expected a single tuple and used destructuring bind to dispatch based on both the method and the tuple's length.

## Dining philosophers and deadlock

     1 new north, south, knife, spoon in {
     2     north(knife) |
     3     south(spoon) |
     4     for (knf <- north) { for (spn <- south) {
     5         "Philosopher 1 Utensils: ".display(knf, ", ", spn, "\n") |
     6         north(knf) |
     7         south(spn)
     8     } } |
     9     for (knf <- north) { for (spn <- south) {
    10         "Philosopher 2 Utensils: ".display(knf, ", ", spn, "\n") |
    11         north(knf) |
    12         south(spn)
    13     } }
    14 }

Makan filsuf masalah memiliki dua filsuf yang berbagi hanya satu set dari perak. Philosopher1 duduk di sisi timur dari meja sementara Philosopher2 duduk di barat. Masing-masing membutuhkan investasi pisau dan garpu untuk makan. Masing-masing menolak untuk melepaskan alat sampai ia telah digunakan baik untuk mengambil menggigit. Jika kedua filsuf mencapai pertama untuk perkakas di kanan mereka, keduanya akan kelaparan: Philosopher1 mendapat pisau, Philosopher2 mendapat garpu, dan tidak pernah membiarkan pergi.

Here's how to solve the problem:

     1 new north, south, knife, spoon in {
     2     north(knife) |
     3     south(spoon) |
     4     for (knf <- north; spn <- south) {
     5         "Philosopher 1 Utensils: ".display(knf, ", ", spn, "\n") |
     6         north(knf) |
     7         south(spn)
     8     } |
     9     for (spn <- south; knf <- north) {
    10         "Philosopher 2 Utensils: ".display(knf, ", ", spn, "\n") |
    11         north(knf) |
    12         south(spn)
    13     }
    14 }

4, 9) The join operator, denoted with a semicolon `;`, declares that the continuation should only proceed if there is a message available on each of the channels simultaneously, preventing the deadlock above.

## Secure design patterns

In this section we describe several design patterns.  These patterns are adapted from Marc Stiegler's [_A PictureBook of Secure Cooperation_](http://erights.org/talks/efun/SecurityPictureBook.pdf).

### Facets

In the MakeCell contract, the client provides two channels, one for getting the value and one for setting it.  If the client then passes only the `get` channel to another process, that process effectively has a read-only view of the cell.  

Saluran seperti `mendapatkan` dan `set` yang disebut "sisi" dari proses. Mereka merangkum kewenangan untuk melakukan tindakan. Jika `set` channel adalah saluran umum seperti `@"Foo"`, maka siapapun yang dapat belajar atau bahkan menebak string `"Foo"` memiliki kewenangan untuk mengatur nilai sel. Di sisi lain, jika `set` channel diciptakan dengan operator `new`, maka tidak ada jalan bagi proses lain untuk membangun `set` channel; itu harus dilalui dengan proses yang langsung dalam rangka untuk proses untuk menggunakannya.  

Note that if `get` and `set` are not created as halves of iopairs, then possession of those channels is also authority to intercept messages sent to the cell:

    for (ret <- get) { P } | 
    for (ret <- get) { Q } | 
    get(ack)

This term has two processes listening on the channel `get` and a single message sent over `get`.  Only one of the two processes will be able to receive the message.

Dengan menerima saluran dari klien untuk mendapatkan dan pengaturan, MakeCell kontrak meninggalkan keputusan tentang bagaimana publik mereka kepada klien. Yang MakeCellFactory kontrak, di sisi lain, konstruksi saluran sendiri dan mengembalikan mereka ke klien, sehingga dalam posisi untuk menerapkan privasi jaminan.

### Attenuating forwarders

Di MakeCellFactory kontrak, hanya ada satu saluran dan pesan yang dikirim secara internal. Untuk mendapatkan efek yang sama sebagai read-only segi, kita dapat membuat sebuah forwarder proses yang hanya mengabaikan setiap pesan yang tidak ingin maju. Kontrak di bawah ini hanya meneruskan "mendapatkan" metode.

    contract MakeGetForwarder(target, ret) = {
        new port in {
            ret(port) |
            contract port(tuple) = {
                tuple.nth(0) match with "get" => target(tuple)
            }
        }
    }

### Revocation

We can implement revocation by creating a forwarder with a kill switch.

     1 contract MakeRevokableForwarder(target, ret) = {
     2     new port, kill, forwardFlag in {
     3         ret(port, kill) |
     4         forwardFlag(true) |
     5         contract port(tuple) = {
     6             for (status <- forwardFlag) {
     7                 forwardFlag(status) |
     8                 match status with true => { target(tuple) }
     9             }
    10         } |
    11         for (_ <- kill; _ <- forwardFlag) {
    12             forwardFlag(false)
    13         }
    14     }
    15 }

2) We create a port to listen for method calls and a channel `forwardFlag` to store whether to forward messages.

3) We return the channel on which clients send requests and the channel on which to send the kill signal.

4) We set the initial state of `forwardFlag` to true.

5-10) We read in an arbitrary tuple of message parts and get and replace the value of the flag.  If the flag is true, we forward the message tuple to `target`.

11-13) If a message is ever sent on the `kill` channel, we set `forwardFlag` to false.  The forwarder process then stops forwarding messages.

### Composition

By combining an attenuating forwarder with a revokable forwarder, we get both features:

    new ret in {
        MakeGetForwarder(target, ret) |
        for (pair <- ret) {
            match pair with [getOnly, kill] => {
                MakeRevokableForwarder(getOnly, ret) |
                for (revokableGetOnly <- ret) {
                    // give away revokableGetOnly instead of target
                    // hang onto kill for later revocation
                }
            }
        }
    }

### Logging forwarder

A logging forwarder can record all messages sent on a channel by echoing them to a second channel.

    contract MakeLoggingForwarder(target, logger, ret) = {
        new port in {
            ret(port) |
            contract port(tuple) {
                target(tuple) |
                logger(tuple)
            }
        }
    }

### Accountability

Misalkan Alice memiliki saluran dan ingin log Bob akses untuk itu. Bob ingin mendelegasikan penggunaan saluran untuk Carol dan log akses nya. Masing-masing pihak adalah gratis untuk membangun mereka sendiri penebangan forwarder di seluruh channel yang telah mereka terima. Alice akan mengadakan Bob bertanggung jawab untuk apa pun Carol tidak.

### Sealing and unsealing

    contract MakeSealerUnsealer(ret) =  {
        new sealer, unsealer, ccRet in {
            ret(sealer, unsealer) |
            MakeCoatCheck(ccRet) |
            for (cc <- ccRet) {
                contract sealer(value, ret) = {
                    cc("new", ret, value, Nil)
                } |
                contract unsealer(ticket, ret) = {
                    cc("get", ret, ticket, Nil)
                }
            }
        }
    }


Sealer/unsealer pasangan memberikan fungsi yang sama dengan kunci publik, tetapi tanpa kriptografi. Itu hanya redaman cek mantel yang dijelaskan di atas. Pola desain ini dapat digunakan untuk menandatangani sesuatu pada nama pengguna. Di Rholang blockchain tutorial ini, kita akan melihat bahwa bahkan bekerja di blockchain karena tidak ada rahasia untuk toko, hanya unforgeable nama-nama yang akan tetap tidak dapat diakses.

### Waspadalah terhadap mengirim attenuators

Prinsip dasar yang perlu diingat dengan RChain proses adalah salah satu yang mirip dengan yang lebih tradisional aplikasi web: apapun kode yang anda kirim ke pihak lain dapat dibongkar. Sejak akhir 1990-an ketika membeli barang melalui web menjadi mungkin, [ada platform e-commerce](https://blog.detectify.com/2016/11/17/7-most-common-e-commerce-security-mistakes/) di mana platform mengandalkan browser pengguna untuk mengirim benar harga barang kembali untuk itu. Penulis tidak berpikir tentang pengguna membuka alat pengembang dan perubahan harga sebelum itu harus dikirim kembali. Cara yang tepat untuk membangun platform e-commerce untuk toko harga pada server dan memeriksa mereka di sana.

Misalkan Bob bersedia untuk menjalankan beberapa kode untuk Alice, dia memiliki kontrak yang mengatakan sesuatu seperti, "Dapatkan proses dari channel ini dan menjalankannya."

    for (p <- x) { *p }

Ini adalah seperti sebuah web browser yang bersedia untuk menjalankan kode JavaScript itu mendapat dari sebuah situs web. Jika Alice mengirimkan Bob pelemahan forwarder, Bob dapat menggunakan pencocokan pola produksi dalam Rholang untuk mengambil terpisah proses dan mendapatkan akses ke sumber daya yang mendasari. Sebaliknya, seperti di e-commerce contoh, Alice hanya harus mengirim kode yang meneruskan permintaan untuk dirinya sendiri proses dan melakukan redaman yang ada.

## Kesimpulan

RChain adalah sebuah bahasa yang dirancang untuk digunakan di blockchain, namun kami tidak menyebutkan apa-apa tentang node, ruang nama, dompet, Rev dan phlogiston, struktur jaringan, atau Casper. Sebuah dokumen yang akan datang akan mengatasi semua masalah ini dan banyak lagi.

Kami berharap bahwa contoh-contoh tersebut di atas memicu keinginan untuk menulis kode yang lebih dan menunjukkan kemudahan mengekspresikan serentak desain.
