# QKD as a Service (QAAS)

[QAAS client API](# QAAS client API ) 

## QAAS client API

### 0x01: `reserveKeyAndGetHalf` request

parameters:

1. endpoint id = `0x01`

2. key length

3. crypto nonce

encoded request example

```
30 0B 02 01 01 02 02 01 00 02 02 30 39
```

explanation:

`30` `0b`: sequence type (`0x30`) with length `0x0b` = 11 bytes;

`02` `01` `01`: integer type (`0x02`) with length `0x01` = 1 bytes, value: `0x01` = 1; ( **endpoint id** )

`02` `02` `01 00`: integer type (`0x02`) with length `0x02` = 2 bytes, value: `0x0100` = 256; ( **key length** )

`02` `02` `30 39`: integer type (`0x02`) with length `0x02` = 2 bytes, value: `0x3039` = 12345; ( **crypto nonce** )

### 0xff: `reserveKeyAndGetHalf` response

returns:

1. error code

2. response id = `0xff`

3. crypto nonce

4. key identifier

5. half of key bytes

6. hash(the other half)

7. hash algorithm id = `0x608648016503040211`

encoded return example:

```
30 23 02 01 00 02 01 ff 02 02 a4 55 04 04 28 8b de 07 04 02 21 a1 04 02 01 02 06 09 60 86 48 01 65 03 04 02 11
```

explanation:

`30` `23`: sequence type (`0x30`) with length `0x23` = 35 bytes;

`02` `01` `00`: integer type (`0x02`) with length `0x01` = 1 bytes, value: `0x00` = 0; ( **error code** )

`02` `01` `ff`: integer type (`0x02`) with length `0x01` = 1 bytes, value: `0xff` = 255; ( **response id** )

`02` `02` `a4 55`: integer type (`0x02`) with length `0x02` = 2 bytes, value: `0xa455` = 42069; ( **crypto nonce** )

`04` `04` `28 8b de 07`: byte array (`0x04`) with length `0x04` = 4 bytes; ( **key identifier** )

`04` `02` `21 a1`: byte array (`0x04`) with length `0x02` = 2 bytes; ( **half of key bytes** )

`04` `02` `01 02`: byte array (`0x04`) with length `0x02` = 2 bytes; ( **hash(the other half)** )

`06` `09` `60 86 48 01 65 03 04 02 11`: object identifier (`0x06`) with length `0x09` = 9 bytes; ( **hash algorithm id** )

### 0x02: `getKeyHalf` request

1. endpoint id = `0x01`

2. key length

3. key identifier

4. crypto nonce

encoded request example

```
30 11 02 01 02 02 02 01 00 04 04 40 af a0 1f 02 02 30 39
```

explanation:

`30` `11`: sequence type (`0x30`) with length `0x11` = 17 bytes;

`02` `01` `02`: integer type (`0x02`) with length `0x01` = 1 bytes, value: `0x02` = 2; ( **endpoint id** )

`02` `02` `01 00`: integer type (`0x02`) with length `0x02` = 2 bytes, value: `0x0100` = 256; ( **key length** )

`04` `04` `40 af a0 1f`: byte array (`0x04`) with length `0x04` = 4 bytes; ( **key identifier** )

`02` `02` `30 39`: integer type (`0x02`) with length `0x02` = 2 bytes, value: `0x3039` = 12345; ( **crypto nonce** )


### 0xfe: `getKeyHalf` response

returns:

1. error code

2. response id = `0xff`

3. crypto nonce

4. half of key bytes

5. hash(the other half)

6. hash algorithm id = `0x608648016503040211`

encoded response example:

```
30 1d 02 01 00 02 01 fe 02 02 30 39 04 02 e1 5c 04 02 01 02 06 09 60 86 48 01 65 03 04 02 11
```

explanation:

`30` `1d`: sequence type (`0x30`) with length `0x1d` = 29 bytes;

`02` `01` `00`: integer type (`0x02`) with length `0x01` = 1 bytes, value: `0x00` = 0; ( **error code** )

`02` `01` `fe`: integer type (`0x02`) with length `0x01` = 1 bytes, value: `0xfe` = 254; ( **response id** )

`02` `02` `30 39`: integer type (`0x02`) with length `0x02` = 2 bytes, value: `0x3039` = 12345; ( **crypto nonce** )

`04` `02` `e1 5c`: byte array (`0x04`) with length `0x02` = 2 bytes; ( **half of key bytes** )

`04` `02` `01 02`: byte array (`0x04`) with length `0x02` = 2 bytes; ( **hash(the other half)** )

`06` `09` `60 86 48 01 65 03 04 02 11`: object identifier (`0x06`) with length `0x09` = 9 bytes; ( **hash algorithm id** )

## QAAS admin API

### 0x03: `getState` request

### 0x04: `setState` request

## 0x05:`getStatistics` request

## Key

# QKD centra darbība

Es `reserveKeyAndGetKeyHalf` un `getKeyHalf` apzīmēšu ar attiecīgi $X$ un $Y$.

Katrs $QKDC$ uzturēs divas datu struktūras $A$ un $B$. $A$ saturēs visas atslēgas, $B$ saturēs tikai tās atslēgas, ko šis $QKDC$ ļauj rezervēt. **Aija** ļauj rezervēt tās atslēgas, kurām baitu summas paritāte ir pāra, bet **Brencis** - tās, kurām paritāte ir nepāra.

Gan $A$, gan $B$ ir `dequeue` datu struktūras, kurām var pievienot elementus gan sākumā, gan beigās, kā arī noņemt elementu gan no sākuma, gan no beigām O(1) laikā. Šajā gadījumā gan A, gan B elementi tiks pievienoti tikai to beigās.

$A$ izmērs jeb $|A|$ ir ierobežots. Maksimālo $|A|$ apzīmēšu ar $W$. Skaidrs, ka $|B|\leq|A|$ pie korektas programmas darbības. 

$QKDC$ darbību var iedalīt trīs notikumos: jaunas atslēgas saņemšana, $X$ pieprasījuma apstrāde, $Y$ pieprasījuma apstrāde.

## Jaunas atslēgas saņemšana

Saņemot jaunu atslēgu $K_{new}$, ja $|A|=W$, tad no $A$ sākuma tiek noņemta atslēga $k_{old}$. Ja $B$ satur $k_{old}$, tad $k_{old}$ atradīsies $B$ sākumā. Ja $B$ sākumā atrodas $k_{old}$, tad viņš tiks noņemts. Šis garantē, ka $B$ nesaturēs elementu, kurš neatrodas $A$. Kad $|A| < W$, $K_{new}$ tiks pievienots $A$ beigās un, ja $K_{new}$ paritāte atbilst šim $QKDC$, arī $B$ beigās.

Lai apstrādātu $Y$ pieprasījumus ir nepieciešams uzglabāt vārdnīcu $D$, kas tiks indeksēta pēc atslēgu `id` un glabās pašas atslēgas. No $A$ izdzēšot atslēgu, tā tiks izdzēsta arī no $D$. 

## $X$ pieprasījuma apstrāde

Apstrādājot $X$, ja $B$ ir tukšs, tad tiek sagaidīts līdz $B$ parādīsies vismaz viens elements. Kad $B$ nav tukšs, no $B$ beigām tiek nolasīta un izdzēsta atslēga $K$. Pieprasītājam, ja $QKDC=\text{Aija}$, tiek nosūtīta atbilde ar $K_{left}$ un $H(K_{right})$. $K$ tiek izdzēsts no $D$.

## $Y$ pieprasījuma apstrāde

Apstrādājot $Y$, kas pieprasa atslēgu $K_{rsrv}$, ja $D$ nesatur $K_{rsrv}$ tiek atgriezta kļūda, ja $D$ satur $K_{rsrv}$, tad, ja $QKDC=\text{Aija}$, tiek nosūtīta atbilde ar $H(K_{left})$ un $K_{right}$.

## Darbības analīze

Pirmo $QKDC$, kurā tiks rezervēta atslēga apzīmēšu ar $P$ un otru $QKDC$ apzīmēšu ar $O$.

Lai mazinātu kļūda skaitu, kas rodas, kad $O$ neeksistē $P$ rezervētā atslēga $K_{rsrv}$ , jāiedala šī kļūda divos gadījumos:

1. $K_{rsrv}$ tika izdzēsts no otra $QKDC$. Šo gadījumu apzīmēšu ar $(1)$.

2. $K_{rsrv}$ vēl nav ticis pievienots otram $QKDC$. Šo gadījumu apzīmēšu ar $(2)$.

$(1)$ kļūdas mazināšanai atslēga $K_{rsrv}$ jātiek uzturētai pēc iespējas ilgāk $O$ no brīža, kad tā tika rezervēta $P$. Tas nozīmē, ka atslēgai jātiek ņemtai no rindas $A$ gala, kurā atslēgas tiek ievietotas nevis ņemtas ārā. Lai rastos kļūda $(1)$, $\text{sender}$ jārezervē atslēga no $P$  ar pieprasījumu $X$ un jāpaiet laikam $T$ līdz $\text{sender}$ pieprasījums $Y$ nonāk pie $O$, kur $T$ ir nepieciešamais laiks, lai $QKD$ uzģenerētu $W$ atslēgas.

$(2)$ kļūdas mazināšanai var neizmantot pēdējās $Z_{count}$ atslēgas no $B$ rinas vai ieturēt kaut kāda laiku brīdi $Z_{time}$ pirms atslēgu var sākt rezervēt, ieviešot vēl vienu rindu, kas glabās $B$ nepievienotās atslēgas. Ja $D$ nesatur $K_{rsrv}$ , manuprāt, nav vērts gaidīt līdz parādās $K_{rsrv}$. Atslēga var vienkārši neparādīties pie kļūdaina izsaukuma vai sistēmas kļūdas.

Ja atslēga tiek norezervēta $P$, bet netiek pieprasīta $O$, tad tā jebkurā gadījumā tiks izdzēsta $T$ laikā, kur $T$ bija nepieciešamais laiks, lai uzģenerēt $W$ atslēgas.
