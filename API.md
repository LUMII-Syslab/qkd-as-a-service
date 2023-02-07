# QKD API

Alice un Bob ir IDQ Clavis3 iekārtas

Aija un Brencis ir Quantum Key Distribution Centres (QKDC), DELL datori

Aija un Brencis realizē divus API: QKD Client API (starp QKD klientu un QKD service) and Synchronization API (nav publisks, izmanto starp Ariju un Brenci)

Divus QKD klientus, kas grib izveidot TLS konekciju ar QKD palīdzību, sauksim par Sender and Receiver (TLS konekciju iniciē Sender) - in order not to confuse them with the client and the server from the QKD Client API viewpoint.

TLS konekciju starp Sender un Receiver sauksim par **TLS with remote QKDC**.

TLS konekciju starp Aiju un Brenci (Synchronization API) sauksim par **TLS with direct QKD**.

Tie būs 2 atšķirīgi TLS papildinājumi no mūsu puses.

Gan Aija, gan Brencis visu laiku saņem atslēgas no Alice un Bob (caur zmq+msgpack) un ieliek atmiņā (līdz 1M atslēgam, kas atbilst 1+ diennaktij) divos “maisos”: pāra un nepāra; vecās atslēgas (pirms pēdējā miljona) tiek automātiski izmestas (e.g., scheduled thread reizi minūtē)

Gan Aijā, gan Brencī darbojas QKDC web service (valodā Go), kas arī realizē QKDC Client API. Turklāt, Brencis pildīs Synchronization API servera lomu, bet Aija pieslēdzas Brencim kā Synchronization API client.

Visi API tiek izmantoti caur web sockets.

# QKDC Client API

Klienti (Sender un Receiver) pieslēdzas QKDC Aijai un QKDC Brencim, kur darbojas HTTPS proxy (HAProxy with open-quantum-safe library, liboqs), kas forwardē koneckijas uz QKDC web service (valodā Go), kas klausās nešifrētu HTTP web socket konekciju un realizē QKDC Client API.

Klienti (Sender un Receiver) validē Aiju un Brenci, pārbaudot, ka to serveru sertifikāti ir parakstīti ar uzticamo CA no ca.truststore (HAProxy sūta visiem klientiem savu servera sertifikātu).

HAProxy uz Aijas un uz Brenča autentificē klientus (Sender un Receiver), pārbaudot to klienta sertifikātus - vai tie ir parakstīti ar uzticamo CA no ca.truststore (tur būs Aijas CA un Brenča CA).

# 

Iespējamas 4 konekcijas: (Sender,Receiver)<—>(Aija,Brencis)

Funkcijas:

- [not used] reserveKey() → id (16 baiti) - it kā būtu loģiski izveidot šādu funkciju, bet lai samazinātu round-trip skaitu, reserveKey() vietā būs nākamā funkcija
- 0x01: reserveKeyAndGetKeyHalf(…) → id, keyL|keyR, hashR|hashL
  - Aija drīkst rezervēt tikai pāra atslēgas (0 mod 2), bet Brencis - nepāra (1 mod 2)
- 0x02: getKeyHalf(id, …)
  → keyL (128 bit), hashR (atgriež Aija)
  → keyR (128 bit), hashL (atgriež Brencis)
  - ja sender mums jau prasīja reserveKeyAndGetKeyHalf, tad getKeyHalf drīkst izsaukt tikai receiver tieši vienu reizi; pēc tam atslēga <id> tiek izdzēsta
  - ja sender rezervēja atslēgu ne pie mums, tad jābūt tieši 2 getKeyHalf izsaukumiem (viens no sender, otrs - no receiver); pēc 2. izsaukuma, atslēga <id> tiek izdzēsta (vai pēc 1 sekundes kopš reserveKey - kas iestāsies ātrāk)

Lai nokodētu funkcijas izsaukumu, kā arī atbildi, mēs izmantosim bināro ASN.1 notāciju. Uz doto brīdi katra ziņojuma garumam jābūt <2+127 baitiem (QKD web serviss to var pārbaudīt).

Lapa, kur var atkodēt ASN.1 ziņojumu: [https://lapo.it/asn1js/](https://lapo.it/asn1js/)

## 0x01: **reserveKeyAndGetKeyHalf**

TODO: extend with the domain name!

Call example:

```bnf
30 0B 02 01 01 02 02 01 00 02 02 30 39
```

30 0B

sequence (0x30), len=0x0B=11 baiti (kopējais garums, neskaitot tekošos 2 baitus)

02 01 **01**

integer (0x02), 1 baits, value = 1 === reserveKeyAndGetKeyHalf

02 02 **01 00**

integer(0x02), 2 bytes long: key length 0x0100=256 bits (in the future, we may need longer keys; QKDs can combine shorter keys, if needed)

02 02 **30 39**

integer(0x02), any number of bytes long (2 bytes in this example): call#

call# is used as nonce as well as a call id; call# must be ≥0 (the highest bit ≠ 1 in 2’s complement)

Notice that reserveKeyAndGetKeyHalf can only be invoked from the sender client; thus, the sender bit is not beeing sent (the TLS handshake with QKD keys is always initiated by the sender)

return:

```bnf
30 48 02 01 **FF** 02 02 **30 39** 04 10 **00 01 02 03 04 05 06 07 08 09 0A 0B 0C 0D 0E 0F** 04 10 **00 01 02 03 04 05 06 07 08 09 0A 0B 0C 0D 0E 0F** 04 10 **00 01 02 03 04 05 06 07 08 09 0A 0B 0C 0D 0E 0F** 06 09 **60 86 48 01 65 03 04 02 11**
```

30 48

sequence (0x30), len=0x48=72=2+3+(4=call#)+(2+16=key id)+(2+16=keyL)+(2+16=hashR)+11(algorithm shake128)

02 01 **FF**

integer (0x20, 1 baits, value = -1 === reserveKeyAndGetKeyHalf result

02 02 **30 39**

integer(0x20), 2 bytes long: call# === nonce (can be longer or shorter ASN.1 integer up to 127 bytes long; must be ≥0, the highest bit ≠ 1 in 2’s complement)

02 01 **00**

integer(0x20), 1 byte long: error code (if non-zero, a web socket will send back also a string message)

04 10 **00 01 02 03 04 05 06 07 08 09 0A 0B 0C 0D 0E 0F**

byte array (0x04, octet string), len=0x10=16: key id (128 biti)

04 10 **00 01 02 03 04 05 06 07 08 09 0A 0B 0C 0D 0E 0F**

byte array (0x04, octet string), len=0x10=16: key[left] (if returned from Aija) or key[right] (if from Brencis)

04 10 **00 01 02 03 04 05 06 07 08 09 0A 0B 0C 0D 0E 0F**

byte array (0x04, octet string), len=0x10=16: shake128(key[right]) (if returned from Aija) or shake128(key[left]) if returnedfrom Brencis

06 09 **60 86 48 01 65 03 04 02 11**

object identifier (0x06), 9 bytes, hash algorithm id: currently shake 128 hash algorithm with NIST id 2.16.840.1.101.3.4.2.11=[2*40+16=96=0x60; 840=0x348=0x86,0x48 [7-bit encoding]; 0x01; 0x65; 0x03; 0x04; 0x02; 0x11] ([https://datatracker.ietf.org/doc/html/rfc8702](https://datatracker.ietf.org/doc/html/rfc8702))

## **getKeyHalf**

Call example:

```bnf
30 1C 02 01 **02** 02 02 **01 00** 04 10 **00 01 02 03 04 05 06 07 08 09 0A 0B 0C 0D 0E 0F** 01 01 **00** 02 02 **30 3A**
```

30 1C 

sequence (0x30), len=0x1C=28

02 01 **02**

integer (0x02), 1 byte, value = 2 === getKeyHalf

02 02 **01 00**

integer(0x02), 2 bytes long: key length 0x0100=256 bits (in the future, we may need longer keys; QKDs can combine shorter keys, if needed)

04 10 **00 01 02 03 04 05 06 07 08 09 0A 0B 0C 0D 0E 0F**

byte array (0x04, octet string), len=0x10=16: key id (128 biti)

01 01 **00**

boolean (0x01), 1 byte long: value=00=false for submitter; FF=true for receiver

02 02 **30 3A**

integer(0x20), 2 bytes long: call# === nonce (can be longer or shorter ASN.1 integer up to 127 bytes long; must be ≥0, the highest bit ≠ 1 in 2’s complement)

return:

```bnf
30 39 02 01 **FE** 02 02 **30 3A** 02 01 **00** 04 10 **00 01 02 03 04 05 06 07 08 09 0A 0B 0C 0D 0E 0F** 04 10 **00 01 02 03 04 05 06 07 08 09 0A 0B 0C 0D 0E 0F** 06 09 **60 86 48 01 65 03 04 02 11**
```

30 39

sequence (0x30), len=0x37=55=2+3+(4=call#)+(2+16=keyL)+(2+16=hashR)+11(algorithm shake128)

02 01 **FE** 

integer (0x02), length=1 byte, value= -2 === getKeyHalf result

02 02 **30 3A**

integer(0x02), 2 bytes long: call#

02 01 **00**

integer(0x02), 1 byte long: error code (if non-zero, a web socket will send a string message)

04 10 **00 01 02 03 04 05 06 07 08 09 0A 0B 0C 0D 0E 0F**

byte array (0x04, octet string), len=0x10=16: key[left] (if returned from Aija) or key[right] (if from Brencis)

04 10 **00 01 02 03 04 05 06 07 08 09 0A 0B 0C 0D 0E 0F**

byte array (0x04, octet string), len=0x10=16: shake128(key[right]) if returned from Aija or shake128(key[left]) if returned from Brencis

06 09 **60 86 48 01 65 03 04 02 11**

object identifier (0x06), 9 bytes, hash algorithm id: currently shake 128 hash algorithm with NIST id 2.16.840.1.101.3.4.2.11=[2*40+16=96=0x60; 840=0x348=0x86,0x48 [7-bit encoding]; 0x01; 0x65; 0x03; 0x04; 0x02; 0x11] ([https://datatracker.ietf.org/doc/html/rfc8702](https://datatracker.ietf.org/doc/html/rfc8702))

**other**

- the server checks the limit on message lengths (currently, ≤2+127 bytes)
- the server ensures thread safety
- the server checks for call# - each subsequent call# from the same user must be greater than the previous

# Synchronization API (deprecated, use Control Protocol instead)

Web socket starp Aiju un Brenci (uz Brenča darbojas PQC HAProxy).

Aija validē Brenci, kuram servera sertifikātam jābūt parakstītam ar uzticamo CA no ca.truststore.

HAProxy uz Brenča autentificē Aiju, pārbaudot Aijas klienta sertifikātu - vai tas ir parakstīts ar uzticamo CA (no ca.truststore).

Synchronization API is needed, since it is costy to restart the QDK process (30 mins for Clavis3). However, sometimes Aija and Brencis web services need to be restarted (e.g., due to maintenance or software/hardware failure). However, we cannot guarantee the exactly same time moment when Aija and Brencis re-join Alice and Bob. Thus, one of Aija/Brencis could have more keys than the other.

Funkcijas:

- 0x04: syncFirstKey(id) → first-common-key-id
  - palaižot Brenci, tas uzreiz mēģina ņemt atslēgas no Clavis;
  - palaižot Aiju, tā mēģina pievienoties Brencim (caur IPSec?)
  - gan Aija, gan Brencis izdzēš atslēgas, kas iegūtas agrāk par first-common-key-id;
- 0x05: mirrorReserveKey(id) → void
  - Aija sūta Brencim pāra atslēgas id, ko rezervējusi, atbildot uz Sender/Receiver reserveKey (ja process sākās pie Brenča, tas sūta Aijai nepāra atslēgas id…)
  - kopš šī brīža, gan Aija, gan Brencis izmet doto atslēgu no atmiņas vai nu pēc diviem getKeyHalf izsaukumiem, vai pēc 1 sekundes (kas iestāsies ātrāk)
  - funkcija ir vajadzīga, jo pēc atslēgas rezervācijas internets var pazust un Brencis nekad neizmetīs atslēgu, ko rezervēja Aija (un otrādi)

## syncFirstKey

```bnf
30 15 02 01 **04** 04 10 **00 01 02 03 04 05 06 07 08 09 0A 0B 0C 0D 0E 0F**
```

30 15

sequence (0x30), len=0x15=21

02 01 **04**

integer (0x02), 1 bytes, value = 4 === syncFirstKey

04 10 **00 01 02 03 04 05 06 07 08 09 0A 0B 0C 0D 0E 0F**

byte array (0x04, octet string), len=0x10=16: key id (128 biti)

return:

```bnf
30 18 02 01 **FC** 02 01 **00** 04 10 **00 01 02 03 04 05 06 07 08 09 0A 0B 0C 0D 0E 0F**
```

30 18

sequence (0x30), len=0x18 = 24

02 01 **FC**

integer (0x02), 1 byte, value = 0xFC === -4 syncFirstKey result

02 01 **00**

integer(0x02), 1 byte long: error code (if non-zero, a web socket will send a string message)

04 10 **00 01 02 03 04 05 06 07 08 09 0A 0B 0C 0D 0E 0F**

byte array (0x04, octet string), len=0x10=16: first-common-key-id

## mirrorReserveKey

```bnf
30 15 02 01 **05** 04 10 **00 01 02 03 04 05 06 07 08 09 0A 0B 0C 0D 0E 0F**
```

30 15

sequence (0x30), len=0x15=21

02 01 **05**

integer (0x20), 1 byte, value = 5 === mirrorReserveKey

04 10 **00 01 02 03 04 05 06 07 08 09 0A 0B 0C 0D 0E 0F**

byte array (0x04, octet string), len=0x10=16: key id (128 biti)

return:

```bnf
30 06 02 01 **FB** 02 01 **00**
```

30 06

sequence (30), len=6

02 01 **FB**

integer (0x02), 1 bytes, value = 0xFB === -5 mirrorReserveKey result

02 01 **00**

integer(0x02), 1 byte long: error code (if non-zero, a web socket will send a string message)

Ik pēc 1 min. (?) Aija un Brencis pārtrauc Synchronization API savienojumu un izveido jaunu (lai nav visu laiku viena un tā pati simetriska atslēga). Pluss, ja ir failure, šis links ātri atjaunojas.

Clavis3 QKD speed: ~400 keys / minute, we can borrow 1 for the synchronization link.

## Vassal KDC

Vassal KDC (VKDC) parazitē uz QKDC; tie krāj atslēgas no īstajiem QKCD Aijas un Brenča un izdala saviem “klientiem”.

Sākumā VKDC1 un VKDC2 darbojas kā Sender un Receiver un dabū vienu simetrisko atslēgu, ko lieto TLS savienojumam starp VKDC1 un VKDC2. Tad VKDC1 ik pa brīdim prasa Aijai vai Brencim (ar varbūtību 1/2) norezervēt QKD key, tad saņem visu atslēgu no Aijas un Brenča (pa pusēm), un nosūta key id VKDC2, kurš arī saņem visu atslēgu no Aijas un Brenča (pa pusēm). VKDC1 un VKDC2 nokešo šādi iegūtas atslēgas, lai izdalītu tās saviem lietotājiem (bet ne mūsu lietotājiem). The rule in Feodal Medieval Europe: “My vassal's vassal is not my vassal”.

Pēc 1 min. (?) VKDC1 vai VKDC2 pārtrauc savu TLS savienojumu, un izveido jaunu (lai nav visu laiku viena un tā pati simetriska atslēga, ar ko šifrē visas pārējās); jaunu savienojumu izveido lazy stilā - tad, kad vajag (kad abu VKDC atslēgu pool nav pilns).

Authorization, Server validation for VKDC: the same.

“Mazu” VKDC var lietot pašos Sender un Receiver, kas bieži komunicē savā starpā,  lai samazinātu round-trip time (atslēga tiek ņemta no lokālā VKDC, kas darbojas uz tā paša datora, nevis uz Aijas vai Brenča);

Bez round-trip benefit, ir vēl šāds benefit: nevajag bieži pārbaudīt Aijas un Brenča sertifikātus, kā arī Aijai un Brencim nevajag katru reizi pārbaudīt Sender un Receiver.
