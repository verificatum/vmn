
/* Copyright 2008-2019 Douglas Wikstrom
 *
 * This file is part of Verificatum Mix-Net (VMN).
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 * BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 * ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.verificatum.protocol.secretsharing;

import java.util.Arrays;

import com.verificatum.arithm.BiExp;
import com.verificatum.arithm.BiPRingPGroup;
import com.verificatum.arithm.HomPRingPGroup;
import com.verificatum.arithm.PRingElement;
import com.verificatum.protocol.demo.DemoError;
import com.verificatum.protocol.demo.DemoException;
import com.verificatum.protocol.demo.DemoProtocol;
import com.verificatum.protocol.demo.DemoProtocolElGamalFactory;
import com.verificatum.protocol.distr.PlainKeys;
import com.verificatum.protocol.elgamal.ProtocolElGamal;
import com.verificatum.ui.UI;
import com.verificatum.ui.info.PrivateInfo;
import com.verificatum.ui.info.ProtocolInfo;


/**
 * Demonstrates
 * {@link com.verificatum.protocol.secretsharing.PedersenSequential}.
 *
 * @author Douglas Wikstrom
 */
@SuppressWarnings({"PMD.SignatureDeclareThrowsException",
                   "PMD.AvoidCatchingGenericException"})
public final class DemoPedersenSequential extends DemoProtocolElGamalFactory {

    // These methods are documented in DemoProtocolFactory.java.

    @Override
    public DemoProtocol newProtocol(final PrivateInfo privateInfo,
                                    final ProtocolInfo protocolInfo,
                                    final UI ui)
        throws Exception {
        return new ExecPedersenSequential(privateInfo, protocolInfo, ui);
    }

    @Override
    public void verify(final DemoProtocol... servers) throws Exception {

        final Object recoveredSecret =
            ((ExecPedersenSequential) servers[1]).recoveredSecret;

        for (int i = 2; i < servers.length; i++) {

            final Object otherRecoveredSecret =
                ((ExecPedersenSequential) servers[i]).recoveredSecret;

            if (!recoveredSecret.equals(otherRecoveredSecret)) {
                throw new DemoException("Recovered collapsed secrets are "
                                        + "not equal!");
            }
        }
    }

    /**
     * Turns {@link PedersenSequential} into a runnable object.
     */
    static class ExecPedersenSequential extends ProtocolElGamal
        implements DemoProtocol {

        /**
         * Secret value shared during the execution of the protocol
         * and then used for testing.
         */
        protected PRingElement recoveredSecret;

        /**
         * Creates a runnable wrapper for the protocol.
         *
         * @param privateInfo Information about this party.
         * @param protocolInfo Information about the protocol
         * executed, including information about other
         * parties.
         * @param ui User interface.
         * @throws Exception If the info instances are malformed.
         */
        ExecPedersenSequential(final PrivateInfo privateInfo,
                               final ProtocolInfo protocolInfo,
                               final UI ui)
            throws Exception {
            super(privateInfo, protocolInfo, ui);
        }

        @Override
        public void run() {
            try {

                startServers();

                final PlainKeys plainKeys =
                    new PlainKeys("DemoSID", this, keygen, rbitlen);
                plainKeys.generate(ui.getLog());

                final boolean[] eliminated = new boolean[k + 1];
                Arrays.fill(eliminated, false);

                final BiPRingPGroup biExp = new BiExp(pGroup);
                final HomPRingPGroup hom = biExp.restrict(pGroup.getg());

                final PedersenSequential peds =
                    new PedersenSequential("DemoSID",
                                           this,
                                           hom,
                                           plainKeys.getPKeys(),
                                           plainKeys.getSKey(),
                                           rbitlen,
                                           true);

                final PRingElement secret =
                    pGroup.getPRing().randomElement(randomSource, rbitlen);

                ui.getLog().info("secret = " + secret.toString());

                peds.execute(ui.getLog(), secret);

                for (int l = 1; l <= threshold; l++) {
                    recoveredSecret = peds.recover(ui.getLog(), l);
                    ui.getLog().info("Recovered secret of " + l + " = "
                                     + recoveredSecret.toString());
                }

                final Pedersen ped =
                    peds.collapse("Collapsed_sid", this, false, ui.getLog());
                recoveredSecret = ped.recover(ui.getLog());
                ui.getLog().info("Recovered secret of collapsed = "
                                 + recoveredSecret.toString());

                shutdown(ui.getLog());

            } catch (final Exception e) {
                throw new DemoError("Unable to run demonstration!", e);
            }
        }
    }
}
