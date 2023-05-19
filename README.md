
# Status
> 🔬 Under Test

Please see the wiki of this repository for full-documentation.

# Rulebook Node
Responsible for managing Exonym nodes, this repository is a core component of the Decentralised Rulebook system.
It provides the infrastructure for Sources and Advocates to deploy rulebooks, and enables the system to operate in a
fully decentralized manner.

The Exonym node management software is designed to be secure, fault-tolerant, and scalable. It ensures that rulebooks
are deployed and executed correctly across the network, and that the system remains highly available and responsive
at all times. The software is responsible for managing system nodes, and for implementing their chosen rulebook.

# Installation

To set up the necessary environment for running the application, we recommend our 
Docker repositories for both `local-` and `deploy-` nodes.  

```
ex0nym/local-rulebook-node
ex0nym/deploy-rulebook-node
```

N.B, that the deploy repository sets up a local static datastore, along with 
TLS and an nginx reverse proxy, while the local repository does not include 
these features.

We are currently working on providing build instructions for this repository. Thank you for your patience.


__TODOs for Production Ready System:__ 

- Active corporate Sybil Registry activated.
- Trustworthy-Sources-Rulebook activated together with the ability to prove honesty to self-list as a Source. 
_______

__&copy; 2023 Exonym GmbH__

This documentation is licensed under the Mozilla Public License, version 2.0 (the "License"); you may not use this file 
except in compliance with the License.

You may obtain a copy of the License at https://www.mozilla.org/en-US/MPL/2.0/.

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
