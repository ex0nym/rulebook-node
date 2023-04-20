# Actors & Objects

Actors and objects are upper case symbols with subscripts 
denoting different actors of the same type.
___
$$
U_A, U_B, U_C
$$

__Actors:__ Alice, Bob, and Charlie.
___
$$
\Lambda
$$

__Object:__ A Rulebook Lambda.
___

$$
\mathbf{\Lambda_S}
$$

__Actors:__ Sources of a Rulebook Lambda.
___

$$
\mathbf{\Lambda_{S,A}}
$$

__Actors:__ Advocates and Sources of the Rulebook Lambda.
___

$$
\mathbf{\Lambda_{0,A}}
$$

__Actors:__ Advocates of a particular Source $\Lambda_0$ of the Rulebook Lambda.  

If there is only one Source, $\Lambda_0$ would be `Advocate0`.  This is
 often the case, because describing problems usually only requires us 
 to consider one Source because all Advocates, no matter the Source, 
 respond in the same way.  This is not always the case and so the 
 comma separated subscripts are necessary.

___

$$
^{H_0}_{\Sigma(p)}U_A
$$

Alice has a Sybil credential of class person, $p$ and is honest under 
a Rulebook, $H_0$.

Proofs of honesty are displayed in the top left position and credentials 
with attributes are displayed in the bottom right position.  Given that
Exonym does not support Attribute Based Credentials in general; it
is almost only Sybil displayed there.

The above can be written more succinctly as

$$
^{H_0}_{\Sigma_p}U_A
$$ 

Sybil class is defined as follows:
- $p$ = `person`
- $e$ = `entity`
- $r$ = `robot`
- $R_e$ =  `representative(entity)`
- $R_p$ =  `representative(person)`
- $P$ =  `product` 

$$
^{H_{1,0}}_{\Sigma_P}Q_0
$$

Above some instance, $0$ of a class of `product` $Q$ is 
honest under Advocate $H_{1,0}$ belonging to Source $H_1$. 
E.g., a 'Fair Trade' rulebook.

They may be honest under many Advocates of the many applicable rulebooks
 and therefore the situation is better written in vector form;

$$
^{\mathbf{R}}_{\Sigma_P}Q_0
$$

$$
\mathbf{R}=[H_{1,0} + H_{1,3} + J_{0}]
$$

The $+$ meaning `AND` and not a summation.


# Transactions
Communication is denoted by the right arrow, with the responses also 
communicated from left to right.

$$
U_A \xrightarrow{m}  U_B \xrightarrow{r}  U_A
$$

Alice sends a message `m` to Bob who responds with `r`.

---

$$
^{H_0}_{\Sigma}U_A \xrightarrow{T_c(\Sigma + H_0)}  U_B
$$

Alice proves honesty under a rulebook $H_0$ and an active Sybil
 credential without revealing her Sybil class.

Note that Bob does not have a Sybil credential because onboarding 
is unnecessary to transact.

The communicated data is written above the right arrow and in this case
communicates a typical proof of honesty. The function $T_c()$ represents
an Identity Mixer type cryptographic proof.  

The subscript $c$ represents a random challenge from Bob, to protect against replay.

The simplest qualifying proof of honesty under rulebook $H_0$ is written 
in full as:

$$
T_c\Bigg(\Sigma() + H_0\Big(V_{H_0}(h_{H_0})\Big)\Bigg)
$$

The function $V(h)$ represents the Verifiable Encryption of the 
Pedersen Commitment to the Revocation Handle $h$.  

The subscript $h_{H_0}$ communicates that $H_0$ is the Revocation Authority.

The subscript $V_{H_0}(h)$ communicates that $H_0$ is the Inspector of the attribute $h$.

For cases where both the inspector and the revocation authority are 
the same as the rulebook Advocate, it is sufficient to write only 
$T_c(\Sigma + H_0)$ because the revocation handle is _always_ verifiably 
encrypted into the proof token.  There may also be systems of Rulebooks 
where third party Inspectors and/or Revocation Authorities are beneficial.

The challenge, $c$ may be a message, $m$ or for non-interactive proofs, $d$ 
for domain.  If it is a random challenge, $c$ must be provided by 
the `Consumer` but this is not necessarily explicit in the notation.

Messages typically also contain a challenge to avoid replay; but $m$ is
used to bind transaction data to the proof token, so
that there can be no doubt that both belong together.

For a non-interactive proof, $T_d(\Sigma + H_0)$ we set $d=$`https://exonym.io/blog` to 
prove honesty under the rulebook $H_0$ for all of the content in the folder `/blog`
of the domain `https://exonym.io`

___ 


$$
U_A  \xrightarrow{\mathbf{B} + c} U_B  \xrightarrow{T_c(\Sigma_{R_e} +H_0 )}  U_A
$$

Alice connects to Bob and provides byte data, $\mathbf{B}$ and challenge $c$.
Bob replies with a proof of honesty under $H_0$

## Pseudonyms
For a natural person to onboard to a rulebook Advocate, users 
provides a slightly different proof to the proof of honesty.

$$
T_c(\Sigma+\mathbf{x_r})
$$

$$
\mathbf{x_r}=nym(\mathbf{r})
$$

The vector $\mathbf{r}$ contains all the rulebook identifiers for 
a given rulebook and the output is the vector of exonyms, $\mathbf{x_r}$.


_______

__&copy; 2023 Exonym GmbH__

This documentation is licensed under the Mozilla Public License, version 2.0 (the "License"); you may not use this file except in compliance with the License.

You may obtain a copy of the License at https://www.mozilla.org/en-US/MPL/2.0/.

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
