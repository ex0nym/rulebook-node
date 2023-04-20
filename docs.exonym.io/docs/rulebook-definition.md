# Rulebook Document
The Rulebook Document is an essential component of a Decentralized Rulebook system that can be applied to govern 
Internet activities within a specific context. 

It is a public point of centralisation that provides an opportunity for consumers to protect themselves 
online while not imposing strict measures that could exclude potential web users. The rules included 
in the rulebook document are meant to be subjective, allowing for inclusive language that everyone 
can agree on. Additionally, the standard for the context should be low to reduce exclusivity and 
ensure the most possible people benefit. 

On this page, we explore the various aspects of the Rulebook Document, including its communication, 
organisation, context, and authorship privileges. We also highlight the polymorphism of rules and 
the role of Advocates in extending the rulebook for specific purposes.

> Tip: __Solve real problems for normal people and accommodate the extremities of society through subjectivity.__

## Communication & Organization

**A Rulebook Document communicates a _subjective_, _low standard_ that the _Internet_ is 
willing to tolerate, within some _narrow context_.**

The immutable part of the rulebook is English-language,
but translations can be included later.

### __Subjectivity__
The aim of a rulebook is to give __Consumers__ the __opportunity__ to 
protect themselves online, it is not to insist that consumer protect 
themselves online.

The rules should be inclusive in that we could not find a law-abiding 
__Producer__ who felt excluded by them.  That's why it needs to be subjective.

> _Everyone agrees they want to be safe, but many disagree on what safe is._

The use of the word _'safe'_ is encouraged __because__ it is subjective and 
users can interpret it.

In authoring the Rulebook Document we are searching for words 
and phrasing that we can all agree on.  We probably won't 
all agree on the meaning of those words.

### __Low Standard__

**Defining a low standard for the context reduces exclusivity.**

If you're trying to solve a problem; focus on the problem and not 
the ideal. Let Advocates and Sources focus on the ideals and the 
vision.

Everyone is a web user and authors need to think of everyone 
who might use it and what they might use it for.

If a `final` rule has a really high-standard for what it's used for, 
people will just ignore the entire Rulebook.  Recall that rulebooks 
are voluntary, which means as an author, you are required to 
make it as beneficial as possible to the most possible people. 
___
__Example__ : Inclusiveness is Key

A control on fake news should target misinformation, not try to 
impose journalistic standards.

Journalistic standards are better applied as an extension of the 
rulebook provided by Advocates.

To make them mandatory at the top document level, results in the 
justifiable publishing of news-like content without a proof of honesty.
This is confusing for Consumers. 
___ 

> __A consumer should be able to look at the mandatory rules of 
a rulebook and feel comfortable that the only outcome from applying 
it, is protection from things they definitely do not want.__

There's no need for high-standards because: 
- Advocates can arbitrarily raise standards with optional (`public`) rules
- Producers can downgrade without penalty on revocation.

With all this said; the rulebook is voluntary and so it can be used 
to enforce high standards without issue.  E.g. a scientific commentary
rulebook.

Problems arise where we can find an honest web user who wants to prove 
honesty but is excluded by proscriptive rules that reduce their 
relevant consumer set size, without providing relevant consumer 
protection.


> _Please ensure you understand the full feature set of rulebooks before
you try writing one._

### __Internet__
Rulebooks allow for users to prove honesty from anywhere on the Internet, 
thereby centralizing control. While this centralized control may be 
perceived as undesirable by some, rulebooks have been intentionally 
designed to prevent centralization of authority.

All rulebooks are voluntary and apply only to specific transactions, 
ensuring that they do not infringe upon any freedoms inherent to the web. 
This is a positive aspect, as it prevents rulebooks from being misused.

### __Context__
Classify a rulebook's usage carefully. Include as many uses as you 
can without adding conflict. Be subjective with terms but specific 
about the activity to resolve conflict. 

> E.g. A free-speech rulebook is incompatible with a news media rulebook 
because although the news media exercises free speech, 
they are held to the higher minimum standard of reporting the truth.  Lying 
is fine in many free speech contexts.  

The conflict arises because the minimum standard is different for the 
two contexts. So ignore free speech and raise the standard to 
__'misinformation'__ and define the rulebook for __'news like content'__.

In contrast a free-speech rulebook, where an Advocate has required 
truthfulness is a reasonable extension.  In news truth is always required 
(hence minimum) and in free-speech truth may be required. 

If we lower the standard to __'disinformation'__, now the Advocates 
need to prove intent and that makes the rulebook too expensive 
to manage.  Misinformation reduces the proof burden for the Advocates
 to having the quality of misinforming the audience.  After all 
 information can be factual and still misinform people because of
 the way it is communicated or who it is communicated to.

Most rulebooks will potentially target billions of transactions and 
so the rules need to be phrased in a way that makes them easy to 
follow and infringements easy for the Advocates to judge. 

### Authorship Privileges
Any Actor may author a Rulebook, independently or as a group; 
however, it is likely that an RFC process is adopted for the 
composition of rulebooks.

# Polymorphism
A rulebook is made up of a set of reusable rules. Rules are made 
reusable via their subjective phrasing and modifiers.

Each rule has a modifier in `["public", "protected", "final"]`. Identified 
phrases also have a modifier in the same set.

> _Any familiarity with polymorphism in programming should 
give a clear idea how it works; however, there is a slight difference 
in meaning to fit our usage context._

## Identifying Phrases
Phrases are identified by local reference using the start- `_[i]` and 
end-phrase `_` operators. They are scoped locally to the rule and zero indexed. 

__Example__
```
"create a reference to _[0]a phrase_ in a rule so that it can be _[1]interpreted_"
```
Both `a phrase` and `interpreted` will be available for Advocates, 
Services, Producers, and Consumers to further define.

## Inheritance
An Advocate establishes by identifying the Source public data.  When 
they identify the Source; they inherit its rules and description.

|||
|---|---|
|`public`|can be ignored and interpreted. |
|`protected`|can be interpreted but not ignored.|
|`final`|can neither be ignored nor further interpreted.|


The default modifier for rules is `public`, except for the zeroth 
rule, which is `protected`.

The default modifier for phrases is `protected`.

Hierarchical precedence is enforced in the following order:

1. Rulebook Document
2. Source
3. Advocate
4. User (Utility, Producer, Consumer)

Lower levels can override only what upper levels haven't restricted.

> Typically `final` restrictions are laws.  It's the imposition that 
user's MUST do something.

The modifiers at the rule level (`public`, `protected`, and `final`) define 
the accessibility and overrideability of the rule as a whole, while 
the modifiers at the phrase level define the 
accessibility and overriding of individual phrases within the rule.

___
__Example__ A `final` rule can have a `public` phrase.

A phrase that can be discarded.

`Something must be A, _[0]B,_ and C.`

By placing a `public` modifier on `B`, Users can discard it or interpret it.

`Something must be A, and C.`
___
__Example__: A `final` rule can have a `protected` phrase.

Consider a rule that _must be implemented_, but there is room for 
_interpretation_. 
___
__Example__: A rule defined as `public` at the Rulebook Document, 
may have further restrictions placed on it by the Source, Advocate, 
Utility, Producer, or Consumer.

Consider that holocaust denial is illegal in many European countries, but 
free speech in the US.

<!-- $$
 \begin{matrix}
   &  &  &  H&  & & \\
   &  & \nearrow & &\nwarrow &  &  \\
   public & H_{US} &  & & & H_{EU} &  final\\
   & \uparrow & &  &  & \uparrow &  \\
  ... & H_{US,0} & &  &  & H_{EU,0} & ... \\
 \end{matrix}
$$ -->


![law inheritance](law-inheritance.png)


The rulebook document may have a `public` rule targeted directly at 
this speech.  A European based Source $H_{EU}$ modifies the rule 
to `final` and Advocates aligning with that Source 
inherit it as `final`.  Now only users agreeing to Advocate interpretations 
with the `final` modifier must agree to it on __Services__ 
that require honesty under that rulebook.

Importantly: a user proving honesty to a global platform 
under an EU Advocate, $H_{EU,0}$ is bound 
by the rule, but for a US Advocate, $H_{US,0}$, they are not.

The platform learns of the rule when they are in development and 
considering which Sources to accept.  This highlights one of the 
network communication qualities of Rulebooks.

> __A global network facilitating regional transactions.__
> 
> A US citizen on Social Media Platform is not necessarily 
bound by the rule and an EU citizen is.  The Social Media platform inherits
the local laws when they accept Sources as authentication.
>
> By selecting an Advocate, the user defines their applicable laws 
and at no ongoing cost to the platform.  The platform only needs 
to consider whether it can handle the restriction.
> This is what actually happens when a global platform accepts
> users from a jurisdiction, it's just poorly represented online.
>
> NB. this scheme requires co-location of Producers & Advocates by 
jurisdiction.

__Without Borders__

This same situation arises politically irrespective of physical 
boundaries.

Consider the interpretation of "spin versus misinformation"
in the news.  One Source may take a very liberal view of 'misinformation',
but restrict the interpretation further than the Rulebook Document defined. 
Under a misinformation rulebook, Fox News and The Financial Times are 
likely to align with different Sources. 

__Pragmatic Enforcement__

There are _a lot_ of transactions to 'police' on the web.  Some might
say it is impossible, but it's not.  Decentralizing controls is a necessary 
start, but we also need the right incentives.

Here we look at some extreme incentives, just to make a point. 

Modifiers and interpretations can also be used to organize users in 
a way that makes enforcement easier.  There are a variety of approaches 
from imposing restrictive rule interpretations to imposing annual 
subscription fees. There will be lots of alternatives and they cannot all be covered here. 

If a set of Advocates $\mathbf{H_{S,A}}$ under a Source were very specific about the meaning 
of the rules.  E.g., to take a __very__ restrictive interpretation of hate 
speech, to be _'anything that potentially directed negativity to another 
person'_.  They would attract a set of users that on infringement
were easy to judge.  More likely still, they would attract a set of 
users who never break the rules.

If a set of Advocates $\mathbf{H_{S,B}}$ want interpretations 
that are very close to what the law will allow, they will likely 
attract a demographic that is frequently reported and have 
a much higher administrative burden.  The burden might be so high that they 
_need to_ charge for their service, which free speech absolutists 
might happily pay for, for the increase in freedom.

Now consider that Advocates $\mathbf{H_{S,A}}$ only allow users with
_'no previous offence'_ and the likely outcome is 
that Advocates will organize, so that only high maintenance users 
will need to pay, and people who behave normally to each other, won't.

When most people log onto Twitter, they don't need to check the 
rules on Hate Speech, because most people have learnt how to behave.

# Composing Rules 

A Rulebook is a JSON document. 
```javascript
{
    urn: "urn:rulebook:" + sha256(concatenate_i(rule_i.urn)),    
    rulebook: [rule0, rule1, ...],    
    language: {
        de[],
        fr[],
    },
    metadata: {
        friendlyName: "Misinformation in the News Media"
    }
}
```

Rulebook metadata MUST contain a `URN` that is automatically 
assigned on finalization of the rules.

Any metadata can be added to the rulebook and is mutable.  This includes 
rule translations.

At the rulebook level a rule is written as follows.  
- There must be a zeroth rule.
- The zeroth rule's minimum modifier is `protected`.  I.e., it must 
be implemented. 

```javascript
let rule0 = { 
	urn: "urn:rule:0:modifier:"+sha256(rule.description)+":rulebookUuid",
	description: "Do not publish _[0]misinformation_ without _[1]timely_, _[2]proportionate corrective action_.",
}
```
When an Advocate inherits the rules, they modify and interpret the 
rulebook as follows:

```javascript
let rule0 = { 
	urn: "urn:rule:0:protected:--assigned--",
	description: "Do not publish _[0]misinformation_ without _[1]timely_, _[2]proportionate corrective action_.",
	modifier: "final"
	interpretation: [{
			modifier: "final",
			definition: "content that misleads an audience that includes factually correct ambiguity."
		},{
			modifier: "final",
			definition: "within 24 hours"            
		},{
			modifier: "protected",
			definition: "Attempts to notify the same audience with the same vigour and enthusiasm as the error was communicated with."
			
	}]
}
```

A rulebook could give users a blank optional rule `_[0]rule_`.

## Immutability
This section discusses the immutability of the rulebook document 
in the context of establishing a rulebook down to first issuance. 

We look at how the enforced immutability of the rulebook document 
is achieved, how each unique rule is assigned a unique rule identifier 
on finalization, and how the rulebook URN is defined as a hashed 
concatenation of the rule URNs. The section also describes how 
the Credential Specification URN can be efficiently and fully 
computed by inspecting the rulebook and how the rulebook is made 
immutable on issuance because of Alice's expectation to prove honesty 
with the Credential she has in her Wallet. 

Additionally, the section notes that the absence of cryptographic 
keys or other systems enforcing the immutability means there is 
nothing to secure, and the rulebook document can be inspected by 
anyone to be valid with respect to a proof they have been shown, 
and anyone can store it, making its resource requirement negligible.

Finally, the section addresses the concern of the rulebook being 
corrupted and explains that the persistence of the data is solved 
by multiple Sources and Advocates inheriting the Rulebook 
via direct copy.

### Enforcing Immutability
To understand how immutability is enforced we need to consider the 
process of establishing a rulebook down to first issuance.

> The enforced immutability of the rulebook document is how we 
> avoided an underlying blockchain and 
> made transactional cost comparable to centralized systems.

$$
H \leftarrow \mathbf{H_{S}} \leftrightarrow  \mathbf{H_{S,A}} \leftarrow ^{H_{0,0}}U_A
$$

Here Alice agreed to the Advocate $H_{0,0}$ from the Advocates in 
$\mathbf{H_{S,A}}$,  however the following steps have taken place
before.

1. Someone wrote the rulebook.
2. Someone finalized the rulebook.
3. Someone installed a Source referencing the Rulebook.
4. Someone installed an Advocate referencing the Rulebook and a Source.
5. Alice agreed to the Advocate's terms and collected a Credential in 
her Wallet.

The Rulebook must have its own `UUID`, so that for any two rulebooks 
containing an identical rule, Alice will still produce unique 
exonyms on Join.

```
rulebookUuid = UUID();
```


On finalization, each unique rule is assigned a unique rule identifier:
```
i = index (0...N)
urn:rule:i:protected:SHA256(rule.description):rulebookUuid
```
With the SHA256() outputting a hexadecimal fingerprint 
of the rule description.

The Rulebook `URN` is defined as a hashed concatenation of the 
Rulebook's rule `URN`s. 

```
rulebookId = SHA256(concatenate_i(rule_i.urn))
urn:rulebook:rulebookId
```
At this point, we can just change the rules and update the 
identifiers without issue.

When a Source or Advocate installs and references the Rulebook, 
they define Specifications and Keys that all contain 
the `rulebookId` in the URN.  They are all checked to be correct
when forming the Source/Advocate relationship, but in principle 
can be inspected to be correct whenever necessary.

The Credential Specification URN can be efficiently and fully computed 
by inspecting the rulebook.

```
urn:exonym:rulebookId:c
```
When Alice claims honesty to Bob, under a Rulebook that Bob has
never seen before, he computes the expected Credential Specification
`URN` by inspecting the Rulebook and performing the above calculations.

_(He checks more, but here we are showing immutability.)_

> __The rulebook is made immutable on issuance because of Alice's expectation to prove honesty with the Credential she has in her Wallet.__

Alice subscribed to a rulebook that is only valid 
for that Credential Specification URN, which is bound to the rules.

_To phrase in Identity Mixer terms_: when an Issuer references the Credential Specification 
and Issues a Credential it creates the expectation of a valid 
Credential Specification URN.  The URN is only valid if the 
Rulebook hasn't changed and therefore the Rulebook is immutable.

The absence of cryptographic keys or other systems enforcing the 
immutability means there is nothing to secure.  Immutability is enforce 
by proxy of the issuance process and that issuer has an interest in 
securing their key. If there are no actors interested in the security 
of the rulebook, the rulebook is retired.

The rulebook document can be inspected by anyone 
to be valid w.r.t a proof they have been shown, and anyone 
can store it, therefore the rulebook document
has a negligible resource requirement.

It can freely exist on the web without interference with the remaining 
weakness being that it can still be corrupted.  An attacker can corrupt it
 by changing or deleting every copy and it would be impossible
to recover the original. 

> __The persistence of the data is solved by multiple Sources and
Advocates inheriting the Rulebook via direct copy.__

If all Advocates and Sources of a given rulebook, agree to update it... 
that would be a new Rulebook, which is fine.

_______

__&copy; 2023 Exonym GmbH__

This documentation is licensed under the Mozilla Public License, version 2.0 (the "License"); you may not use this file except in compliance with the License.

You may obtain a copy of the License at https://www.mozilla.org/en-US/MPL/2.0/.

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
