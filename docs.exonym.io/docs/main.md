> __Tip__
>
> If you're new to Exonym, we recommend watching our series 
> of [short-videos](https://exonym.io).
> 
> There is quite a lot of documentation for this project, but by understanding 
> this first page, the reader should be able to see that the solution is fit for purpose.
>

# Understanding Exonym
We believe in a self-governing web where everyone can have a voice in decision-making.

Exonym facilitates the separation of utility from governance so that web users 
can work together to build a safer, fairer web. 

## Aim
To enable effective accountability for anonymous transactions on the open web.

## Overview
Exonym and Decentralised Rulebooks is a Transactional Governance framework. 
A Rulebook is defined with a narrow context and can be 
subscribed to voluntarily by anyone, and used to govern specific transactions. 

The system is based on the notion of "Proving Current Honesty" and 
allows anyone to fill any role.  Breaching a rulebook results in a 
loss of privilege, and accountability is imposed only when the 
offender tries to rejoin. 

A rulebook is decentralized between Sources, and 
Advocates who form a strong reputation system and the right to govern 
is established through these links.

Next we'll look at a specific example of how this framework can be applied in practice.

### Rulebooks as a Method for Enforcing Regional Standards
The web needs a solution to the problem of global governance 
with regional regulations and we use advertising standards to 
illustrate that. 

By deploying Rulebooks, a country can regulate its own 
advertising standards on the web in a 
way that is incredibly low maintenance.

The country installs a Rulebook node, which generates a small 
amount of static data and that simple step is all that's needed
to be able to repatriate their advertising standards from 
private enterprise. 

This is achieved by establishing the country as a Source 
and digital advertising agencies in their own country as Advocates. 
It's a bit like regional licensing. In this case a license to 
enforce advertising standards for ads targeted at that region.

The Source maintains a list of agencies they trust to follow and enforce 
their standards, and the Advocates self-regulate to advertise in their 
own country, just like they do today.

When advertising in another country, they agree to comply with 
the regulations of that country by subscribing to one of their 
Advocates.

The system enables countries to escape 
the current paradigm of relying on private enterprise to enforce 
their standards. Anyone who wants to advertise in that country 
subscribes to one of the advocates. When actions are in the same 
country, the government can apply penalties as there's already an 
existing government structure. However, when someone from a different
country breaks the rules, a report to the Advocate results in privilege revocation
for that agent and now they can apply even penalties to
anyone who breaks the rules, no matter where they are in the world.

The solution enables greater regional control and accountability, 
and is an innovative approach to a complex governance problem.

> There is a Rulebook configuration point to note about this scheme.
In this case the system ensures that an advertiser 
who loses their privilege under one Advocate cannot switch to 
another Advocate, but only within the same Source (country).
This approach allows advertisers to advertise in multiple 
countries, and if they violate the regulations of one country, 
it only affects their privilege in that specific country. 

## Transactional Concept
<sup>[Notation Guide](notation.md)</sup>

Let's get into the weeds.

$$
  U_A \xrightarrow{T_c(\Sigma + H_0)} U_B
$$


Bob wants to know that Alice is honest.  He might be buying something, 
imposing T&Cs onto Alice, or just accepting a link where he needs to 
trust the information.

Alice proves Current Honesty to Bob under an Advocate's interpretation 
$H_0$ of a Decentralised Rulebook $H$.

If Alice cheats, Bob shows evidence to $H_0$ who can stop 
Alice from claiming honesty to other peers.

If Alice tries to rejoin the rulebook at $H_1$, she will need to prove 
'exonyms' under the rulebook's rule identifiers, at which point she 
reveals that she has broken rules and $H_1$ can impose a penalty.

> __Assertion__
> 
> _Given that 'Proving Current Honesty' is as simple as pushing a button, the transaction 
> is simple enough to be accessible to all web users._
>

## Governance Concept
- Transactional governance as a benefit.  A control must be useful to one or more parties in a transaction to apply.
- Automated onboarding.  Agreeing to a rulebook may be done without permission.
- Breaching a rulebook may result in loss of privilege. 
- Only when the alleged offender tries to rejoin can accountability be imposed.
- Each rulebook applies to a narrow context to make them easier to define and manage.
- Each rulebook is written as the lowest standard that we should be willing to accept.
- Interpretations of the rulebook increase the standard to what Sources and Advocates believe is desirable.
- Controllers must maintain reputation.
- Strong incentives and disincentives for controllers.
- A sovereign nation can repatriate governance from global platforms for their citizens.
- Governance is bilateral,  (utilities can also agree to the user's terms.)
- Alice subscribes to rulebook interpretations appropriate to her own values.
- Alice communicates her values to peers by her subscription choices.

## High Level Properties
- __Permissionless__: anyone can fill any role.
- __Voluntary__: if there's a safer, cheaper, or fairer way to get the transactional security you need - use it.
- __Federated__: Decentralised Rulebooks can accommodate the world's diversity.
- __Liberal__: anything within the law is allowed.
- __Democratic__: there's no voting, but the more extreme a user's choices the smaller their available audience.
- __Peer-to-peer__: identities are self-sovereign and transactions are p2p.
- __Anonymous__: no two proofs can be shown to be from the same user.

## [Product Path](product-path.md)
See dedicated page.

# Structure
Let $H$ be an arbitrary Rulebook with Sources $\mathbf{H_S}$ Advocates 
$\mathbf{H_{S,A}}$ and a Producer $U_C$.  We define a Rulebook to be structured 
as follows:

$$
    H \leftarrow \mathbf{H_S} \leftrightarrow \mathbf{H_{S,A}} \leftarrow U_C
$$

A Rulebook is referenced by one or more Sources, who inherit its rules.  Advocates 
link to the Source they most align with, who links back to them and again inherit 
its rules.  This establishes a shared ruleset and the Source/Advocate reputation system.

Charlie, $U_C$ who is a 'Producer' chooses the Advocate he most aligns with and 
without interacting with the Advocate, anonymously proves honesty to peers and 
utilities on the open web.

Consumers and web utilities (services) select and further interpret useful Rulebooks 
to fit the transactional context.  They deselect Sources and individual Advocates 
who they do not trust.

Producers agree to terms under an Advocate that is acceptable to the counterpart(s).  

The right to govern is established as follows:
- A Source asserts an interest in governing the web within some narrow scope.
- Advocates grant the Source the right to govern in that context, by linking to them.
- A Producer grants the Advocate the right to govern, by subscribing to them.
- A Producer only exposes themselves to governance when they show the credential.
- A Producer allows an Advocate to impose a penalty by identifying themselves.

> __Assertion__
>
> For any Rulebook that defines a sufficiently narrow context, anyone has the right to govern the web.
> 

## Enforcing National Advertising Standards & Whitebox Advertising
> This section goes into more detail than the introduction. 
 
Digital advertising can be a tricky business, with varying advertising 
standards across countries and the potential for dishonesty in ad 
placements. To combat these issues, we can deploy a Rulebook with 
a single rule: _"Follow advertising standards."_ 

Each country's advertising regulatory 
body installs a Source that references the Rulebook. Every digital 
advertising agency in a country (such as Germany) then installs an 
Advocate that references their country's Source. The Source then 
links back to the Advocate to confirm their validity. 

Each agency registers their employees as representatives of the 
agency, and each Advocate enables automated onboarding. Any ad 
targeted at a specific country must be submitted to a whitebox 
database, along with a proof of honesty. 

Upon uploading the ad, the ad service (such as GoogleAds) confirms its presence on the 
whitebox of each targeted country. The ad placement service then 
checks the advertiser's proof of honesty and inserts it into the 
metadata of the ad for user reporting.

To participate in this system as an employed domestic ad agent, 
one must subscribe to their employer's Advocate. Independent 
domestic ad agents can subscribe to an Advocate in their own 
country, and foreign ad agents can subscribe to an Advocate in 
the target nation of the ad campaign. Each ad agent subscribes 
to one Advocate for each country they advertise in.

It's important to note that this system can be implemented by 
individual countries and does not require participation from 
the entire world. 

By simply setting up the Sources and Advocates, advertisers 
can prove that they will comply with local advertising 
regulations and that they will be honest with respect to them. This could reduce 
fraudulent advertising practices and create a more trustworthy online advertising ecosystem.

It would change the way online advertising is regulated and enforced 
by providing a decentralized and permissionless system for 
verifying compliance.  It would reduce the reliance on centralized 
authorities and create a more open and transparent advertising environment. 

### The bare bones of the example

- We define a Rulebook $F$ with a single rule of `"Follow advertising standards."`  
- Each country's advertising regulatory body installs a Source referencing $F$.
- Every Digital Advertising Agency in a country, (Germany (DE)) installs an Advocate, $F_{DE, i}$ 
and links to their country's Source, $F_{DE}$
- The Source links back to them to confirm they are a valid Advocate.
- Each agency registers their employees as representatives of the agency.
- Each Advocate enables automated onboarding.
- An ad targeted at a country, MUST be submitted to a whitebox database, together with a proof of honesty.
- On upload of the ad, the ad service (e.g. GoogleAds) confirms 
the ad's presence on the whitebox of each targeted country.
- The ad placement service checks the advertisers proof of honesty and 
inserts it into the metadata of the ad for user reporting.

As an __employed domestic ad agent__: subscribe to the employers Advocate.

As an __independent domestic ad agent__: subscribe to an Advocate in their own country.  I.e., an Advocate in the set of $\mathbf{F_{DE}}$.

As a __foreign ad agent__: subscribe to an Advocate in the target nation of the ad campaign.

Each ad agent subscribes to one Advocate for each country they advertise in.

For many use cases, revocation from an Advocate belonging to one Source 
would require revocation across many Advocates under different Sources.
In this case, Sources are independent (by sovereign nation).  If the ad 
agent had subscribed to many Advocates (ad agencies) under one Source (DE), 
the chain revocation protocol would prevent the ad agent from proving honesty.

> _N.B. we say "each country" here, like the whole world needs to participate for it to 
work.  This is not the case: one country can act alone._

__Fit for Purpose__
- For all cases, each ad agent is accountable to the government of the targeted nation. 
- The scheme does not create barriers to entry.
- Isolated to a single country, the market is structured like this today.
- There's minimal administration for both Sources & Advocates.
- It does not solidify the position of incumbents. 
- All agencies can advertise globally.
- If an agent is revoked, they can no longer advertise in that region. (Sybil prevents it.)
- The agent can advertise elsewhere and so extra-judicial overreach has been avoided.


## [Rulebook](../specification/rulebook-definition.md)
A Rulebook defines one or more subjective rules to be interpreted by Sources and Advocates. 

To maximize reusability Rulebooks support polymorphism for both rules and phrases.  

It can be authored by anyone and when one Producer agrees to the rulebook, it becomes immutable.

## Sybil, $\Sigma$
A clone resistant onboarding service.  Passing clone checks results in the issuance of 
a cryptographic credential that binds the user to a set of unique identifiers and 
commits the identity to a class (`Person`, `Robot`, etc ...).

Sybil facilitates corporate structures, so that when a user does something online, 
they are not personally accountable for what they do for their employer.

Minors lack responsibility for their actions and so they act as a representative 
of their parents.

## Source
A Source provides the broad strokes of a given Rulebook.  Their purpose is to accept 
Advocates who they trust to interpret and extend the rulebook. Their role is mostly organizational.  

Penalties are defined at the Source level rather than the Rulebook level, so that they are
 federated.  The logic is that if a Source can lower penalties without increasing administration 
for their Advocates, that Source is more effective and their approach should be adopted.

## Advocate
Advocates interpret subjectivity in the rulebook and extend it with rules 
they believe increases Consumer protections without diminishing Producer freedoms 
for the given network activity.

## Producer
A web user who adds to the Internet.  Producers subscribe to Advocate interpretations 
of rulebooks and through doing so agree to consumer protections. 

## Consumer
A consuming web user.  Consumers select all Sources, deselects Sources and individual 
Advocates who they don't agree with.

The concept is that Consumers select all Sources and Advocates and then blacklist 
controllers when they have an opinion. 

## Utility
A utility or service provider.  E.g. Amazon, Google, Stripe, etc.

## Source-Advocate Reputation System w.r.t Consumer Protections
The relationship between the Source and the Advocate is based on 
trust and cooperation. The power balance is not based on a hierarchy 
but on mutual agreement between the parties.

The Source provides the basic guidelines for the Rulebook, which are 
then interpreted and extended by Advocates who the Source trusts 
to follow the rules and ensure consumer protection.

The Advocate's job is to add more rules to the Rulebook, which 
will increase consumer protection without limiting Producer freedoms.

The Producer agrees to follow Advocate interpretations of the 
Rulebook, which ensures consumer protection.

The Consumer selects all Sources and Advocates, then removes 
any they disagree with.

Penalties are defined by the Source to allow for a decentralized 
penalty system. If a Source can lower penalties without adding 
work for their Advocates, their approach is considered more 
effective and should be followed.

___
__Note on Internet Governance and Governments__

Exonym is fit for purpose because it requires very little change
from our current organizational structures.  Our 
societies are very well set-up to support good governance.  

What's missing is the ability to communicate and organize _on_ the
 Internet _about_ what controls are and are not wanted.

What's needed is the ability for people and companies to organize and 
solve their own Internet Governance problems, before the Governments 
_have_ to get involved.

We need to do those things and allow the web to remain 
__open, permissionless, and distributed.__

> It is unlikely that there is a liberal democratic country on 
Earth that _wants_ to be involved with Social Media.  They 
_have_ to be because it is a dumpster fire that leads to serious 
harms and loss of life.  

___

# System Overview
The Exonym network is a collection of [nodes](node.md) that generate, publish and broadcast 
cryptographic data to increase trust in anonymous transactions.

Sources and Advocates are incentivized to run nodes because they want to add their 
interpretation of the rules to the Internet.  (E.g. interpretations of the First 
Amendment of the US Constitution on Social Media.)

An underlying distributed ledger registers Source/Advocate data<sup>[1]</sup>.  Each Node 
exposes an API and maintain an exonym map  of rulebook activity.  Producers and 
Consumers interact with others via their self-sovereign Wallet. Services request and 
verify proofs via an API.

Rules are freely composed in a JSON document.  There is an immutable part (the Rulebook 
that Sources reference) and a mutable part (the interpretation and extension of the rules).

A Rule Origination System secured with multiple Rulebooks opens the proposition and 
composition of rules to all actors, whether they are controllers or not.

___ 
__[1] Note on Blockchains__

The framework being __permissionless__ is more important to the underlying use case 
than using blockchain technologies. We have therefore rooted to certification authorities 
rather than deploying a ledger until blockchain technologies become a viable path 
forward. 
___

## Identity Mixer
At the center of Exonym is an extraordinary cryptographic library from IBM Research 
called Identity Mixer.  Identity Mixer has features that the W3C Verifiable 
Credentials specification does not have.  Additionally, the library has 
features that Hyperledger Indy's AnonCredentials does not have.

__Roles__

- Issuer
- Prover
- Verifier
- Revocation Authority
- Inspector

__Features__

- Pseudonyms
- Revocation
- Tertiary revocation authorities
- Attribute commitments
- Attribute predicates
- Verifiable encryption
- Tertiary inspectors

 > We refer to Scope-Exclusive Pseudonyms that use the scope of a rule identifier as an 'exonym' 
 > meaning 'external name'.  They are Pedersen Commitments without a blinding factor.

## Exonym & Attribute Based Credentials

Identity Mixer is an Attribute Based Credential (ABC) system but Exonym does _not_ support ABCs in general because:  

- Our core offering is a permissionless governance framework.
- There are many alternative self-sovereign ABC tools available.
- Identity Mixer is non-compliant with the Verifiable Credential W3C standard as it pre-dates it.
- There is too little benefit for the cost of implementation.

## Decentralization vs Singularity: Rethinking Transactional Integrity
The Internet is made up of many interconnected networks, and 
Exonym may seem like an attempt to 
create a singular solution, but that's not the case. 

It's important to recognize that there are many situations 
where trust can be established in other ways, such as limited 
centralised control, smart contracts, or blockchain transactions.

However, there are still many cases where broad centralized 
controls are unfortunately necessary, such as in content moderation, 
package management, and cross-jurisdictional trade. 

The problem is that relying on centralized systems can lead 
to them becoming our governors, which may not always be desirable
and that is the problem we solve.

In the end, the decision to use Exonym or not should be 
based on whether it benefits the specific transaction at hand.

# Patents
`Patents Pending (US/EU)`


> 
> __Exonym's benefits are available to all natural people, for free.__  For 
> companies and representatives, we charge a one-off Sybil registration fee.
> 


Exonym has pending patents in both the US and EU, which serve as a 
solution to the hard-fork problem.

Exonym's code repositories are licensed under a modified MIT 
license that includes a requirement for users to utilise our 
clone management service (Sybil). However, this documentation is 
licensed under the Mozilla Public License (MPL).

While software patents can be a nuisance in many cases, they 
are necessary in this case, so that we can govern clone accounts 
to ensure that the rulebooks remain effective.

The registration fee for companies and representatives is a one-time 
Sybil fee, while Exonym's benefits are available to all natural 
people for free. 

Exonym is a software system that needs revenue to survive, 
and the monetization of corporate registrations provides a 
fair model for creating a revenue stream without undermining 
the system's permissionless properties. 

The revenue generated is a 
function of employment churn. The centralization of power is 
minimized by this model, making it the best option among 
several alternatives.





_______

&copy; Copyright 2023 Exonym GmbH

This documentation is licensed under the Mozilla Public License, version 2.0 (the "License"); you may not use this file except in compliance with the License.

You may obtain a copy of the License at https://www.mozilla.org/en-US/MPL/2.0/.

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
