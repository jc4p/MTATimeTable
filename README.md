MTATimeTable
============

An Android app to help me go to work and back.

Backstory
=========

I take the MTA every day to work (and most days back) but I really hate spending extra time waiting under ground. I'd rather be home in my nice air conditioned room and go underground only when I know the train's about to come. Granted, the subway is often messed up but luckily I live on the C train which is around 90% accurate to its schedule.

This app let's me say "Okay Google, Start MTA" to my watch (or open the phone app), determines if I'm on my way to work or home, then tells me how many minutes until the next train that'll take me there.

Lots of code clean-up should happen soon. I initially thought I could access my `IntentService` directly from my Wearable application so I moved the main logic to a "shared" subproject, but now only the main mobile project touches it so it's not shared at all.
