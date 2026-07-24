I want components such as Upscale and Downscale, which currently support wId == 0 only, to support multiple IDs. Of course, this is not going to work by default. Here is the idea:

- Have a IdSerialize, which is a component that ensures that outstanding transactions have only one ID by stalling. It is kinda like TransactionTracker, but just a single component (similar to connect, if you will)
- Then, place this IdSerialize:
  - wId == 0 -> no need for it
  - wId != 0 --> have an option idSerialize = true/false, which places it
  - If there is no ID serializer, maybe we can check for it during simulation? SimulationCheck options for it maybe?
  - Or, maybe we can have some elaboration time checks (axi4 interface with a property outstandingThreads == 1 to make sure?)
  - Maybe we can have a separate module called SingleIdChecker? What do you think?
- Let's discuss