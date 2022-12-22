# Quantum Key Distribution Centre darbība

Es `reserveKeyAndGetKeyHalf` un `getKeyHalf` apzīmēšu ar attiecīgi $X$ un $Y$.

Katrs $QKDC$ uzturēs divas datu struktūras $A$ un $B$. $A$ saturēs visas atslēgas, $B$ saturēs tikai tās atslēgas, ko šis $QKDC$ ļauj rezervēt. **Aija** ļauj rezervēt tās atslēgas, kurām baitu summas paritāte ir pāra, bet **Brencis** - tās, kurām paritāte ir nepāra.

Gan $A$, gan $B$ ir `dequeue` datu struktūras, kurām var pievienot elementus gan sākumā, gan beigās, kā arī noņemt elementu gan no sākuma, gan no beigām O(1) laikā. Šajā gadījumā gan A, gan B elementi tiks pievienoti tikai to beigās.

$A$ izmērs jeb $|A|$ ir ierobežots. Maksimālo $|A|$ apzīmēšu ar $W$. Skaidrs, ka $|B|\leq|A|$ pie korektas programmas darbības. 

$QKDC$ darbību var iedalīt trīs notikumos.

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
