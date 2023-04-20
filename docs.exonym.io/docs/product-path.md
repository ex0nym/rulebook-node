# Product Path
The focus in the early to mid term is: 
- collaborate with interested parties to customize rulebook implementations for particular applications
- to make Rulebooks as accessible to non-technical people as possible. 
- improve Sybil tooling to maximize the benefit to corporations.
- improve performance and scalability, so that a rulebook could theoretically rapidly onboard billions of users.
- Provide a "Rule Origination" framework on a shared blockchain.

As the web3.0 technology space progresses we intend to bolster 
data availability using an underlying blockchain.

# User Registration (Blockchain)
The ideal Exonym deployment utilizes both blockchain's immutable 
and double-spend prevention properties.

Immutability guarantees that data hasn't been tampered with. Micro-payments
are useful for distributing penalties and to fund the composition
of new rules.

Whenever a financial penalty is imposed it will ideally distribute to 
the network, so that the amount is meaningful for the person who needs 
to pay and negligible for the Advocate who imposes it.

Apart from immutability and double-spending, we need
the additional properties to be consistent with a decentralized
governance system and our environmental goals.

The underlying blockchain we use must have the following qualities:
- Permissionless
- Control cannot be purchased (Consensus Algorithm)
- Energy efficient (Consensus Algorithm)
- A stable underlying currency (Value)
- Anonymous user registration (Blackbox Governance)

Exonym places a priority on being permissionless and open. We have
 dedicated substantial effort and made significant sacrifices to 
 establish ourselves in this manner. 
 
 Deploying a ledger on a permissionless blockchain is impeded by 
 the possibility of a takeover path, which could result from the 
 acquisition of tokens or hardware. As a result, Exonym has chosen 
 to build on the more stable foundations rooting in 
 Certification Authorities.

 While it's true that Certification Authorities have had their own 
 share of issues, including security breaches and fraudulent 
 certificates, they still offer a more secure alternative to 
 the potential risks of a takeover path on a permissionless blockchain. 
 
 While no system is foolproof, the risks posed by a takeover path 
 are far more significant, making Certification Authorities a better 
 option for the short-term.

## Issues
The blockchain technology space is in so much flux that there isn't 
a clear choice of what ledger technology is most suitable for Exonym.

Here is a summary of the issues that should be resolved.

### Value
If we want to implement payments, we need a cryptocurrency.  

Rulebooks apply to both web2.0 and web3.0 governance 
and a cryptocurrency could alienate many of the web2.0 use cases for 
many users.  The perception would be false, but it would be a real 
perception.

Pegging the price to a fiat currency is a possibility, but it's not clear
cut.  Consider Exonym pegging to CNY (Chinese Yuan) and the political 
implications quickly become apparent.  Choosing EUR, USD, or CHF has much 
the same connotations for some people.

It's not that today's China would ever use Exonym; it's that foreign alignment 
is a big deal. We might shrug this off, but it's only because we're so good 
on our own side that makes it _easy_ rather than _correct_. 

If the entire world invested in a foreign currency it gives that currency issuer 
incredible political power. It doesn't matter that we've done it 
already. It matters that as 'systems-caretakers' for future generations,
we don't further solidify this position.

Using BTC is too volatile for the use case and history has already
shown us what happens when a crisis tightens its grip around an 
arbitrary Rarity/S&D model. (End of the Gold Standard).

If we ever issue a payment token, the underlying will be a 
low volatility representation of value with a published 
formulaic derivation.

The more likely outcome, especially in the mid-term will be our 
alignment with an existing ledger and we use their native token;
despite its flaws but at the time of writing, there are no 
obvious choices.

By establishing Decentralized Rulebooks as chain agnostic and 
without a native currency, we demonstrate that as a system of 
governance; it is as distributed as the web and any future 
alignment with a blockchain can be undone as well as done.

### Single Asset Investment and Centralization
When many people put their money in one thing, it can 
lead to dishonest behavior and having too much power 
in one place, which goes against the idea of being decentralized.

### Blackbox governance
Using a consistent identifier that is linked to your device or identity 
can act as a stand-in for your real identity. This allows others to 
apply governance or profiling without your knowledge. Although this 
is a common practice on the web, it goes against the principles of 
liberal democracy.

### On-chain user registration
To fully realize Decentralized Rulebooks we require Garman's 
'Decentralized Anonymous Credentials' (DAC), so that user's can 
self-register and perform an anonymous key exchange without the 
possibility of a man-in-the-middle (MIM) attack.  

At the time of writing the DAC isn't efficient enough for 
production use<sup>[Citation Needed]</sup>.
 This only limits p2p use cases to already trusted 
 communication channels, and so it's a nice-to-have 
 looking at the big picture.

 > This inefficiency significantly de-prioritizes the ledger requirement.

The proof tokens are all anonymous.  As long we can secure a
communication channel (usually via TLS), then the user is anonymous
, save for IP Address.

In the absence of a DAC, any kind of on-chain user registration 
results in the 'black-box governance problem' where
users have a consistent identifier that are linked to an identity.

We could still register Sources and Advocates and their key data, which
would be a benefit; however, Exonym nodes require TLS certificates anyway,
and so rooting to CAs is acceptable from a security point of view. 

The main advantage of a ledger would be increased data availability. 


### Consensus Algorithms
Proof-of-Work may not be as decentralized as it seems as it is possible 
for someone to buy over 50% of the network's computing power. Similarly, 
a staking mechanism where the user with the most money has the most 
control would not be suitable for a governance system. 

Permissioned ledgers are controlled by definition and can be purchased. 
Although controlling the network may not necessarily lead to control of the rules, 
it is a likely outcome.

## Conclusion
> In the absence of a DAC, the lack of ledger isn't important.  
> 
> There's little missed opportunity because there isn't one.
>
> There are no serious failures from using TLS.

Mitigating MIM attacks via TLS is by far the safest way to deploy Exonym
, because it can't easily be taken over.

Today's Blockchain approaches are only superficially acceptable, because 
they all have finance at the base of it and financial incentives 
distort our morals and practices.

There isn't currently a suitable blockchain system for Exonym and the 
concessions made by using one far outweigh the benefit of high 
data availability, which can be solved in other ways.

_______

__&copy; 2023 Exonym GmbH__

This documentation is licensed under the Mozilla Public License, version 2.0 (the "License"); you may not use this file except in compliance with the License.

You may obtain a copy of the License at https://www.mozilla.org/en-US/MPL/2.0/.

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
