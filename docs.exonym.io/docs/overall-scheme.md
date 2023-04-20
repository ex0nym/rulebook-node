# Exonym & Decentralized Rulebooks 
Exonym and Decentralized Rulebooks together provide a governance framework that 
enables trustworthy transactions without centralized control.  

Anyone can create a Rulebook for a particular topic and advocate 
for it, which can be joined by any interested party. 

The Rulebook is structured with a focused yet subjective Rulebook Document, 
and then Sources and Advocates extend and interpret it. The Source references 
a set of baseline rules that are immutable and inherited by its chosen Advocates.

Anyone can subscribe to a Rulebook by agreeing to follow its rules when 
they show it to Consumers, which is when the rulebook applies. This 
subscription model, combined with the ability for anyone to create and advocate 
for a Rulebook, makes the system of rulebooks permissionless.

Producers join the rulebook at an Advocate whose interpretation is closest 
to their own values and then they prove "current honesty" under it to Consumers.

As Producers we expose ourselves to penalties, if we don't follow the rules, 
because rejoining the same rulebook at any Advocate can be detected and 
importantly, without compromising privacy. 

Decentralized rulebooks hold great potential for creating an Internet 
governance model that is transparent, accountable, and community-driven, 
and that can be scaled up and implemented cost-effectively.
________

**References:**  [Roles](roles.md), [Notation](notation.md), [FAQ](faq.md#exonym-smart-contracts-and-blockchain)

________


__Definitions:__ 

|Term|Definition|
|---|---|
| Decentralized Rulebooks | A distributed software system facilitating multi-stakeholder governance.|
| Rulebook | An identified rulebook document, the set of all Sources referencing that document, and the set of all Advocates trusted by the rulebook's Sources. |
| Exonym | A decentralized cryptographic identity system that enables Decentralized Rulebooks. |
| _- an exonym_ | _A distinct cryptographic pseudonym for a distinct rule identifier._ |
| Sybil | A centralized, closed source, clone resistant user onboarding system. |

A distinction between systems of Decentralised Rulebooks and Exonym is needed 
because combined they can be used to control online activties and the 
core thesis of Decentralised Rulebooks is to avoid centralized 
control of online activities.

'**Decentralized Rulebooks**' are a _governance system_ that facilitate 
___user accountability___ by rooting governance for a specific 
Internet activity in a public, immutable **rulebook document**.
Each **Rulebook**, requires Producers to onboard to Exonym. 
Consumers are not required to onboard.

Exonym is an _identity system_ that facilitates ___anonymity___.  Exonym's Sybil 
service resists unauthorized clone accounts and breaks the link between a user's
real-world identity and their Exonym identity.

Exonym, in combination with Sybil, is used to govern clone accounts, 
actively managing the governance of their activities and other 
capabilities. Rulebooks govern where they are applied and wanted 
by consumers. By working together, Exonym and Sybil, along with 
Rulebooks, allow for a new world of transactional trust where 
transactional utility is separate from transactional governance 
and governance is owned by interested web users.

> We can refer to 'Exonym' or 'Rulebooks' colloquially as the identity system, Sybil, and the 
set of all rulebooks.

_____
For a deeper dive into the [processes and philosophy](process.md) see the dedicated page or the 
[introductory materials](main.md). This page focuses on the informatics.
_____
# Identity Mixer as a Basis
Exonym uses Identity Mixer (IDMX), which is controlled through a set 
of XML documents.  They can be published to distributed web servers, 
read, and passed to methods that verify third-party 
attested __Credential Attributes__.  

> To use Exonym you must agree to the Identity Mixer license agreement as well as
> Exonym's License Agreement,

The cryptosystem is bound by __System Parameters__. 
If another IDMX implementation used the same system parameters, 
all issued credentials would be compatible.

__Issuers__ are defined by __Issuer Parameters__, which conform and are 
bound to __Credential Specifications__.  The Issuers issue the credential 
attribute values in accordance with the Credential Specification 
via __Issuance Tokens__ to __Credential Owners__.  The Issuers execute 
credential issuance via an __Issuance Policy__. Within the 
Issuance Policy is optionally a __Presentation Policy__ and if present, 
it represents an issuance flow that requires that the Issuer 
first acts as a __Verifier__.

What is to be verified is defined via a __Presentation Policy__, or 
rather a set of __Presentation Policy Alternatives__.  The Verifier 
states all the possible alternatives that are acceptable to them 
and the __Owner__ only proves that they fulfill one of them. 

Proving ownership of a Credential takes place via 
a __Presentation Token__ which allows the 
Verifier to impose different flavours of pseudonym onto the 
Prover. It also provides features to cryptographically prove 
inequalities and set membership.
```
(18 < age < 65, or {(person OR representative) AND NOT (entity OR robot)})
```

The library also allows for the designation of __Inspectors__ in 
the Presentation Token, who are trusted to reveal values that 
are embedded into tokens, but only under set circumstances. E.g. Here 
is my credit card details, and a company can read 
them for processing when this token is presented.  I.e., I 
never share my credit card details with stores, I share them 
only with one payment processing company.  The cryptographic 
protocol is called __Verifiable Encryption__ and allows a third-party 
to check that a specific value has been encrypted with a specific 
key, without revealing what the value is.

The library offers a rich set of features which Exonym uses, 
with the exception of Verifier Driven Revocation, which allows a 
Verifier to revoke a credential, so that they are only allowed 
to show it once.

 ### W3C's Verifiable Credentials Specification
Identity Mixer predated both the W3C Verifiable Credentials 
Specification and the Verifiable Claims Working Group, however
none of the subsequent projects based on the specification have
the breadth of features that Identity Mixer has. 

Standards reign supreme when they provide the necessary functionality
however, Decentralised Rulebooks need features that are yet to 
be provided by the spec.  

For this reason we do not facilitate bespoke Attribute Based 
Credentials (ABC), nor do we plan to until we can comply 
with the standard. If you want the ABC facility, you should use a 
different project. 

There are many Verifiable Credential providers who abide by the specification;
but you will need to observe their respective governance 
structures, which are centralized. They are centralized at the minimum 
at a system level and in some cases at an institutional level.

## Scheme Overview
> Recall: - Any web actor can take on any role.

Let $H$ be an arbitrary Rulebook with Source's $\mathbf{H_S}$ Advocates 
$\mathbf{H_{S,A}}$ and a Producer $U_C$.  We define a Rulebook to be structured 
as follows:

$$
    H \leftarrow \mathbf{H_S} \leftrightarrow \mathbf{H_{S,A}} \leftarrow U_C
$$

### Sources
The public [verifiable rulebook document](../specification/rulebook-definition.md) is 
referenced by one or more Sources, who inherit its rules by direct copy. When 
established they generate and publish a set of machine verifiable 
data $\mathbf{\chi_{H, S_0}}$ that binds their Node $H_{S_0}$ to rulebook $H$.

### Advocates
Advocates link to the Source they most align with by URL.  The Source's Exonym Node 
is verified by inspecting $\mathbf{\chi_{H, S_0}}$.  Once verified, they generate and publish 
$\mathbf{\chi_{H, S_0, A_0}}$, which binds the Advocate to the Source and the rulebook document.

The Advocate sends their URL to the Source and if and only if the Source accepts them, 
does it establish a shared ruleset and the Source/Advocate reputation system.

The Advocate publishes their interpretations and extensions of the rulebook together 
with rule identifiers, $\mathbf{r}=[r_0, r_1, ..., r_N]$, and can 
make the subscription available openly, or require access criteria. (Payment, membership 
of another rulebook, complete a questionnaire, ...)

Charlie, $U_C$ who is a 'Producer', has not previously joined the rulebook at 
any $\mathbf{H_{S,A}}$, and has already [onboarded](../sybil/sybil.md) to Sybil $\Sigma$, subscribes 
with the interactive proof:

$$
U_C \xrightarrow{T_c(\Sigma_{*} + \mathbf{x_r})} H_{S_0, A_0}
$$

 

$$    
  \mathbf{x_r} = [nym(r_i)]
$$

The function $nym(r_i)$ produces an exonym for the rule identifier $r_i$.  The Advocate 
checks their Exonym Map and if the exonyms are clean they issue them a credential, $H_{S_0,A_0}$ and broadcast
a `join` message to all listeners $\mathbf{H_{S,A}}$, who update their Exonym Maps.

### Producers
Once Charlie has $H_{S_0,A_0}$ in his wallet, he can _"prove current honesty"_ without 
interacting with the Advocate.  The proof is anonymous, in that another proof created by
Charlie cannot be inspected to have been created from the same identity. He can prove 
ownership of one or more credentials, interactively or non-interactively.

$$
U_C \xrightarrow{T_c(\Sigma_{*} + H_0)}  U_A 
$$



Interactive proofs require the verifier generates a random challenge, $c$ to protect against
replay attacks. Usage examples are as follows:

- on sign-up to a service.
- on sign-on to a service.
- on performance of a privileged action.
- on transaction with another peer.

Non-interactive proofs can be written to a URL for third-party inspection, or contain the 
transactional data that the proof is attesting to and is written either 
$T_d(...)$ or $T_m(...)$.  $d$ is the domain or specific URL that the proof applies to. 
$m$ is the byte data being transmitted and the proof token becomes a wrapper around the message
and the message is bound to the proof. Usage examples are as follows:

- on publication of content to a web server.
- on publication of content to a service.
- on send of an anonymous data packet.
- on reference to a third-party.

Non-interactive proofs are extraordinarily powerful if you consider inserting JSON data for 
an attestation of a single piece of static content published to a arbitrary web server.

```
const d = {
    'url':'https://exonym.io/adult-cartoon.html',
    'whitelist':['adult'],
    'blacklist':['religious']
}
```
As a Producer who has subjected myself to governance, I am 
potentially freer than I was as a Producer with total freedom.  This is because 
we can facilitate rules that require the classification of some appropriate content 
types, so that all the _"I've got children"_ arguments disappear
in the face of explicit filters that are _effective_.  This allows our safe, communal use 
of public amplification settings that host both minors an adults. 
If the onus is on the publisher to follow tagging rules or face accountability, the 
entire information sphere becomes freer.

> One day we imagine a world in which _cartoonists_ can _try_ to be _funny_ without being _murdered_.

### Consumers
> Recall:
> - Consumers do not need to onboard to Exonym.
> - Proofs are consumed either by peers (end users) or utilities (web services)

Peers and utilities select and further interpret useful Rulebooks 
to fit the transactional context. 

This is a muddy world of complexity that is comparable to the complexity of building 
web services. While p2p use cases are wonderful, ease of transaction and more importantly 
trustworthy transactions is the user's objective. 

We consider peer-to-peer transactions as transactions between people who have an 
existing relationship, that requires some extra trust. For all other transactions, 
we consider them to take place on utilities or via non-interactive proofs.

Selection is via a direct copy and merge of each Sources' signed Presentation Policy.  So, 
they select all and deselect Sources and individual Advocates who they do not trust. A local
blacklist is maintained, so that the Consumer's method of verification doesn't need to 
repeat actions and they can define default-deny or default-accept protocols for each source.

Verification is via installable software, a browser plug-in, or the Exonym 
wallet (App Store / Play Store).

Interpretation is optional as the Advocate has already interpreted a default, which both
counterparties can modify if they wish.

Producers agree to terms under an Advocate that is acceptable to the Consumers.  
___

__Example__

Consider an Internet service that facilites Asset Transfer between the EU and UK. There
are lots of requirements from identity disclosure to content disclosure, to anything
else that's needed to legitimize the transaction. There may be multiple rulebooks that
can be applied with strict interpretations on the rulebooks' rules that allow the 
service to operate legally.  And there is a lot of work in setting all that up, from 
software development, to process, to compliance.

- Anyone who uses the service is a Consumer and potentially a Producer. Producers sign-up to the service with the necessary proofs.
- The service is a Consumer.  
  
The Service selects and interpret rulebooks to ensure their clients can transact 
safely.  The only difference between today's Internet services solving regulatory
requirements and solving regulatory requirements with rulebooks; is that 
the rulebooks solve only the governance, are portable and highly reusable, 
and governance applies to many services.
___

### Infringement
If Charlie cheats, Alice sends the proof token with evidence of cheating 
to $H_0$, who can prevent Charlie from proving honesty to anyone else.

For Charlie to join another Advocate, $H_1$ will require him to produce exonyms 
under the rule identifiers.  Here we look at the informatics of this process 
in detail. 

Peers are issued a revocable credential by a host and then prove honesty by 
showing the credential. The act of showing a credential to prove honesty and 
of obtaining the credential are two distinct process.  

To join a rulebook, $H$ Alice produces a token:

$$ T_c(\Sigma + \mathbf{x_r}) $$

To prove honesty under $H_{s,a}$ Alice produces a token:

$$ T_c(\Sigma + H_{s,a}(V(h_{H_{s,a}}))) $$

$V(h_{H_{s,a}})$ verifiably encrypts the revocation handle $h_{H_{s,a}}$ into
the proof token, so that $H_{s,a}$ can use their Inspector key to read Alice's 
specific $h$ and update their Revocation Information.  On update Alice can 
no longer prove honesty, and must either lose privilege rights or re-join 
the rulebook. 

There are two issues to solve:
1. To preserve privacy, Alice cannot be discovered to have been revoked without Alice revealing it.
2. When Alice chooses to reveal previous infringement, she must also reveal which rule she broke.

For efficiency, these conditions must remain a two-party transaction and complete in a single proof 
token, so that Alice is either denied or accepted.  If denied, she triggers the requirement for 
accountability.

__Conditions__
- Within a Rulebook there are lots of interpretations of the same rules and 
each node is entitled to extend the rulebook. 
- A Producer can subscribe to multiple Advocates in the same Rulebook. (Upgrades and downgrades)
- All Advocates and Consumers must implement the zeroth rule $r_0$
- A user subscribes to _all_ rules for their choice of Advocate.

> We don't consider rule origination here as it is a layer-3 system - Exonym is a layer-2 system.

Recall
- by the binding of credential ownership to an underlying secret and the prevention of credential 
pooling, Alice cannot borrow credentials.
- Alice can produce exactly one unique exonym for each rule identifier.

Consider Advocates controlling for rules, as follows:

&emsp; $H_0 := \begin{matrix}[r_0 && r_1  && r_2]\end{matrix}$

&emsp; $H_1 := \begin{matrix}[r_0 && r_1]\end{matrix}$

&emsp; $H_2 := [\begin{matrix}r_0 && r_1  && r_3 \end{matrix}]$

#### Exonym Matricies
Each node maintains two public exonym matricies; $\mathbf{X}$ and $\mathbf{Y}$
for controlled and uncontrolled rules, respectively.  Explictly:

$$
\mathbf{X} = \left[
 \begin{array}{ccc}
  r_0 & ... & r_N \\
  x_{0_{Alice}} & ... & x_{N_{Alice}} \\
  \vdots & \vdots & \vdots  
  \end{array}
\right]
$$

When Alice joins an Advocate, they publish Alice's undiscovered exonyms to 
the controlled matrix and discovered exonyms, to the uncontrolled matrix.

They store the mapping $x_0 \rightarrowtail h_A$ in their database; i.e. An advocate
can discover a revocation handle of a user from the zeroth exonym.

On downgrade, Alice can still obtain the credential; but all exonyms are 
published to $\mathbf{Y}$.

A join message is broadcast to the Rulebook and each node updates their Exonym 
Map. Exonym Maps are a shared index that allow the computation of a URL where the 
Exonym Matricies can be found.

> N.B., If there are flagged rules, Alice has been revoked and she is denied the credential.

We have solved 'Issue 1'.  Alice is not forced to join an Advocate, instead she 
can choose privilege loss.  If she wants to regain the privilege, she must reveal 
the exonyms at which point she will identify which rule she was revoked for. In the 
absence of third-party systems, if Alice chooses not to reveal the exonyms, 
her actions are indeterminate by ANY web user. This includes all Advocates, 
Sources, and Sybil. Her privacy cannot be infringed.

#### Preamble to Multi-Advocate subscriptions
Rulebooks need to be reusable and practical and so we allow for upgrades
and downgrades.  

A user upgrades when they want access to a rule for a specific transaction, where 
their current Advocate(s) don't control for that rule.  E.g., I can prove honesty
under a freedom of expression rulebook, $H_1$ but I want to write in a forum that minors
also use and I need to agree to the higher standard of not using any profanities ($r_3$), $H_2$.

To reverse the issue: I might already have agreed to $H_2$ and while I _can_ enter
a forum with $H_2$ where $r_3$ does not apply; $H_2$ says something about me that 
I might not want to reveal in the transaction.  I.e., I have previously posted in 
forums that requires $r_3$.

An analogy is that I can both drink in a pub 
and attend a white-tie dinner.  My behaviour will be appropriate 
to the environment; but if I show up to the pub wearing a tuxedo, I may be received
differently than I would have if I was dressed like everyone else.  It's the 
mirroring principle.

Most users don't want to think about what Advocate they're subscribing to.  Most 
will tend to think more about it after an experience that they don't want to 
encounter again.  So "today's forum" might have low standards that they later need 
to upgrade, or high-standards that they might not necessarily want to reveal in 
tomorrow's forum.

To solve this a user can subscribe to an Advocate with lower standards than one 
they are already honest under.

> If two Advocates control for the same rules, the user cannot join.


#### Multi-Advocate Management
To move from one rulebook to another will always be detected via the 
common rule $r_0$.  Without a common rule, it is a different rulebook.

|Case|Advocate Migration|Origin Exonyms|  Destination $\mathbf{X}$ | Destination $\mathbf{Y}$ |
|---|---|---|---|---|
| 1 |$H_0 \longrightarrow H_1$|$[\begin{matrix} x_0 & x_1 & x_2 \end{matrix}]$ | $[\begin{matrix} 0 & 0 \end{matrix}]$|$[\begin{matrix} x_0 & x_1 \end{matrix}]$|
| 2 |$H_0 \longrightarrow H_2$|$[\begin{matrix} x_0 & x_1 & x_2 \end{matrix}]$|$[\begin{matrix} 0 & 0 & x_3 \end{matrix}]$|$[\begin{matrix} x_0 & x_1 & 0 \end{matrix}]$|
| 3 |$H_1 \longrightarrow H_2$|$[\begin{matrix} x_0 & x_1 \end{matrix}]$|$[\begin{matrix} 0 & 0 & x_3 \end{matrix}]$|$[\begin{matrix} x_0 & x_1 & 0 \end{matrix}]$|

Cases 1 & 2 have distinct rules at the origin. Cases 2 & 3 have distinct rules at 
the destination.

For Case-1 if Alice is revoked for rule 2, she can join as she is 
downgrading.   For any other revocation, her join request is denied.

For Case-2 if Alice is revoked for rule 2, she can join as she is
upgrading, but without $r_2$.  If Alice is not revoked, she can join
and $H_2$ will only control for $r_3$.  If Alice wants to prove honesty
under $r_2$ and $r_3$ she produces the token, $T_c(\Sigma + H_0 + H_2)$.
For any other revocation, her join request is denied.

For Case-3 if Alice is revoked for any origin rules, Alice cannot join, as 
$H_2$ will be interpreted by consumers as her being currently honest 
under $r_0$ and $r_1$.  Without revocation, Alice upgrades and $H_2$ controls
only for $r_3$.

### Revocation
Our objective is to revoke Alice's credential(s), so that for the rule(s) that she 
is being revoked for; a token claiming honesty cannot be interpreted as being
currently honest under that rulebook.

We must consider that Alice might have produced a token under an Advocate that does
not control for the rule that she infringed. We therefore consider the transaction:

$$
^{[H_0 + H_1 + H_2]}U_A  \xrightarrow{T_c(\Sigma + H_2)} U_B
$$

I.e., Alice is currently honest under $[H_0 + H_1 + H_2]$ and claims honesty to
Bob under $H_2$ for $r_0$, $r_1$, and $r_3$.  She joins the Advocates 
without prior revocaton, in the 
order $H_1 \rightarrow H_0 \rightarrow H_2$ to produce the matrix rows:



&emsp; $\mathbf{X_{H_1}} = \begin{matrix}[x_0 & x_1]\end{matrix}$

&emsp; $\mathbf{X_{H_0}} =  \begin{matrix}[0 && 0  && x_2]\end{matrix}$

&emsp; $\mathbf{X_{H_2}} = [\begin{matrix}0 && 0  && x_3 \end{matrix}]$


If Alice is reported by Bob (with evidence) for $r_3$, she is reported to $H_2$ who
controls for that rule.  They can revoke and broadcast a revoke message, without any 
other Advocate performing any operation.  She can still prove honesty under $H_0$ and $H_1$.

If Alice is reported for $r_1$ (or $r_0$) there are two outcomes: 
1. $H_2$ responds to Bob with the forwarding Advocate, $H_1$ and does __not__ revoke $H_2$
2. Bob sends the token to $H_1$, and $H_1$ may revoke.
3. If and only if they revoke: they broadcast a revoke message that contains a representation of $x_0$.
4. Advocates not controlling for $r_1$, where their proof can be interpreted as being 
honest under $r_1$ will be revoked. 

In this case, all of them.  However if $H_1$ only controlled for $r_0$, Alice's
$H_1$ credential would remain intact.

Advocates can execute the revocation by looking up the revocation handle via 
the private mapping that was saved when she joined their node: 
namely,  $x_0 \rightarrowtail h_A$ 

### Accountability
Rulebooks rely on accountability to ensure that 
participants follow the agreed-upon rules. Penalties are imposed 
when a producer fails to adhere to the rules, and the following 
options available;

- financial penalty
- time penalty
- ownership of a credential
- total exclusion from a specific Source (restricted)

Although it may seem restricted initially, the ownership 
of a credential is highly adaptable and may necessitate 
a variety of activities to be completed before Alice can 
reclaim access. This is comparable to community service, 
where the individual must complete specific tasks before 
being granted access to certain privileges.

The restrictions placed on total exclusion from a specific 
source are twofold:

- First, there must be another source available for the producer 
to join. This ensures that the producer is not completely cut 
off from all sources and still has the opportunity to engage 
in transactions. 

- Second, there must be at least one source that has not activated 
the permanent exclusion penalty. This means that even if a 
producer is excluded from one source, they can still potentially 
access other sources that have not imposed this strict penalty. 

One potential application of the total-exclusion penalty would 
be to establish a free-to-join network that attracts well-behaved users who 
don't require much attention. However, producers who frequently 
push the boundaries of acceptability may be barred from joining 
such a network if they've previously been excluded under specific 
rules that are high-risk.

This approach could be particularly beneficial 
for content moderation, where AI can handle routine cases for 
free, while commercial subscription Advocates can handle the 
more complex cases for producers who operate closer to the 
limits of what is permissible.


_______

&copy; Copyright 2023 Exonym GmbH

This documentation is licensed under the Mozilla Public License, version 2.0 (the "License"); you may not use this file except in compliance with the License.

You may obtain a copy of the License at https://www.mozilla.org/en-US/MPL/2.0/.

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
