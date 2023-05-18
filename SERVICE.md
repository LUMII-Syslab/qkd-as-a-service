# Operation of QaaS service

QaaS consists of two services hosted by Aija and Brencis. These services are reffered to as KDCs (Key Distribution Centers).

The operations of reserveKeyAndGetKeyHalf (denoted as $X$) and getKeyHalf (denoted as $Y$) are fundamental for these KDCs.

## Concepts and Definitions

Each KDC maintains two dequeue data structures—$A$ and $B$ as well as a dictionary $D$.
A dequeue data structure is a special type of queue that allows elements to be added and removed from both ends efficiently in O(1) time.

$A$ stores all added keys while $B$ only contains those keys which the KDC permits to reserve.
Aija permits to reserve keys with even byte sum parities.
Brencis which reserves keys with odd byte sum parities.

The size of $A$, or $|A|$, is bounded, and we'll denote its maximum value as $W$.
Consequently, $|B|$ should always be less than or equal to $|A|$ for the program to operate correctly.

## Details of Operation

In essence, the operation of QaaS can be classified into three primary scenarios: obtaining a new key, processing an 'X' request, and handling a 'Y' request.

1. **Obtaining a New Key**

    When a new key, denoted as $K_{new}$, is obtained, it's checked against the maximum limit $W$. If the size of the data structure $A$, denoted as $|A|$, equals $W$, an older key $k_{old}$ is removed from $D$ and the start of $A$. If data structure $B$ contains $k_{old}$, it will also be removed from $B$. This ensures that $B$ does not contain any elements not found in $A$. When $|A| < W$, $K_{new}$ is added to the end of both $A$ and $B$, provided the parity of $K_{new}$ aligns with the KDC. The parity of a key is determined by the sum of its bytes. Elements present in $A$ are indexed in $D$ by the key's id. When a key is removed from $A$, it's also removed from $D$.

2. **Processing an 'X' Request**

    During an 'X' request, the system checks if $B$ is empty. If it is, the system waits until $B$ has at least one element. Once $B$ isn't empty, a key $K$ is read and removed from the end of $B$. The requester then receives a response containing $K_{left}$ and the hashed value of $K_{right}$, denoted as $H(K_{right})$, provided the KDC matches with Aija. The key is then removed from $B$.

3. **Handling a 'Y' Request**

    A 'Y' request requires a reserved key, $K_{rsrv}$. If dictionary $D$ doesn't contain $K_{rsrv}$, an error is returned. However, if $D$ does contain $K_{rsrv}$, and the KDC matches Aija, a response containing $H(K_{left})$ and $K_{right}$ is sent.

**Performance Analysis and Optimizations**

To ensure optimal performance and minimize errors, it's essential to analyze the system operation and identify potential improvements.

One potential issue occurs when the other KDC, denoted as $O$, does not contain a key $K_{rsrv}$ reserved by the first KDC, denoted as $P$. This issue can be divided into two scenarios:

- Case 1: $K_{rsrv}$ is deleted from $O$. This situation can be minimized by maintaining $K_{rsrv}$ in $O$ as long as possible after it's reserved in $P$. Specifically, the key should be removed from the end of queue $A$, where new keys are added, not from the start. This strategy minimizes the chance of an error, assuming a sender reserves a key from $P$ with an 'X' request, and the time $T$ elapses before the sender's 'Y' request reaches $O$. The time $T$ represents the time needed for the KD system to generate $W$ keys.

- Case 2: $K_{rsrv}$ hasn't been added to $O$ yet. To mitigate this issue, the system could avoid using the last $Z_{count}$ keys from the $B$ queue or introduce a waiting period $Z_{time}$ before keys can be reserved, by maintaining an additional queue to store unadded keys. If $D$ doesn't contain $K_{rsrv}$, it's arguably inefficient to wait for $K_{rsrv}$ to appear, as it might never appear due to a faulty call or a system error.

By analyzing these scenarios and implementing potential improvements, QaaS can provide a robust, secure, and efficient key distribution service in an economically feasible manner.
